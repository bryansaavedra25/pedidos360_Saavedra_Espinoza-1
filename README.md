Pedidos360
Plataforma de gestión de pedidos y productos construida con enfoque Cloud Native. En la Evaluación Parcial N° 2 se escaló el sistema del Parcial 1 incorporando una capa de mensajería asíncrona con Amazon SNS y SQS: crear un pedido responde de inmediato y las tareas secundarias (notificar, facturar, actualizar inventario) se procesan en segundo plano, sin alterar la lógica de negocio existente.
Asignatura: Desarrollo Cloud Native I (DSY1107) · Duoc UC
Integrantes: Bryan Saavedra (@bryansaavedra25) · Elena Espinoza (@EAikeE)
Contenido del repositorio
Monorepo con los tres componentes del sistema:
Carpeta
Descripción
Tecnología
Puerto
backend-pedidos/
API REST de pedidos y productos. Productor de eventos y consumidores SQS
Java 17, Spring Boot 3.3.4
8080
microservicio-admin/
Microservicio administrador de colas y tópicos
Java 17, Spring Boot 3.3.4
8081
frontend/
Dashboard con login Cognito
React 18 + Vite
5173
Arquitectura
Frontend (React + Amplify)
   │  login                         ┌──────────────┐
   ├──────────────────────────────► │ AWS Cognito  │ (User Pool, JWT)
   │  Authorization: Bearer <JWT>   └──────────────┘
   ▼
API Gateway ──► backend-pedidos (Spring Boot, valida el JWT)
                    │                         │
                    │ guarda                  │ publica evento (eventType)
                    ▼                         ▼
             ArangoDB Cloud            SNS: pedidos-topic
                                              │ fanout + filtros por eventType
                 ┌────────────────────────────┼────────────────────────────┐
                 ▼                            ▼                            ▼
        notificaciones-queue        pedidos-creados-queue      inventario-actualizado-queue
           (DLQ propia)                 (DLQ propia)                 (DLQ propia)
                 │                            │                            │
        NotificacionesConsumer       FacturacionConsumer          InventarioConsumer

microservicio-admin (API REST) ──► crea/elimina colas y tópicos, consulta estado, purga DLQ
Amazon CloudWatch ──► métricas de las colas + alarma sobre las DLQ
Mensajería con SNS y SQS
Recursos en AWS (región us-east-1)
Tópico SNS: pedidos-topic, con tres suscripciones SQS con Raw Message Delivery activado.
Cola (función)
Visibility timeout
Retención
DLQ
maxReceiveCount
Retención DLQ
notificaciones-queue
30 s
1 día
notificaciones-dlq
3
14 días
pedidos-creados-queue (facturación)
60 s
4 días
pedidos-creados-dlq
5
14 días
inventario-actualizado-queue
30 s
2 días
inventario-actualizado-dlq
3
14 días
Cada cola enlaza su DLQ mediante Redrive Policy.
Patrones de enrutamiento
Fanout: un evento PEDIDO_CREADO publicado una vez en el tópico llega a todas las colas suscritas.
Entrega selectiva: cada suscripción tiene una Filter Policy sobre el atributo eventType.
Cola
Filter Policy (eventType)
notificaciones-queue
sin filtro (recibe todos)
pedidos-creados-queue
PEDIDO_CREADO, PEDIDO_COMPLETADO
inventario-actualizado-queue
PEDIDO_CREADO, PEDIDO_ELIMINADO
Productor
PedidoService no usa el SDK directamente: delega en PedidoEventPublisher, que publica en el tópico el pedido en JSON con el atributo eventType (PEDIDO_CREADO, PEDIDO_COMPLETADO, PEDIDO_ELIMINADO). Si la publicación falla, se registra en el log y no se interrumpe la respuesta HTTP.
Consumidores (agrupados por dominio)
backend-pedidos/src/main/java/com/pedidos360/backend/messaging/
├── PedidoEventPublisher.java        # productor
├── ProcesadorMensajes.java          # ACK, reintentos, duplicados y envío a DLQ
├── DlqConsumer.java                 # registra en logs los mensajes de las 3 DLQ
├── notificaciones/NotificacionesConsumer.java
├── facturacion/FacturacionConsumer.java
└── inventario/InventarioConsumer.java
Los consumidores usan @SqsListener con acknowledgementMode = "MANUAL". El manejo de errores de ProcesadorMensajes es:
Situación
Comportamiento
Mensaje duplicado
Se consulta el messageId en ArangoDB (colección de mensajes procesados); si ya existe, se confirma y se descarta
Error no recuperable (cuerpo ilegible)
Se envía a la DLQ con el id original y la causa, y se confirma
Error recuperable
No se confirma; SQS reentrega tras el visibility timeout hasta agotar maxReceiveCount y luego pasa a la DLQ
Éxito
Se registra el mensaje como procesado y se confirma (ACK)
Los logs de reintento y de carta muerta usan el formato id=<messageId> intento=<n> causa=<error>, y DlqConsumer registra con el mismo id que se ve en la consola de SQS.
Fallo simulado para la demo: un pedido con cliente = "FALLAR" hace fallar al consumidor de inventario, lo que permite mostrar los reintentos y la llegada a la DLQ.
Configuración centralizada
Nombres del tópico y de las colas en backend-pedidos/src/main/resources/application.properties (aws.sns.pedidos-topic-arn, aws.sqs.queues.*).
Clientes SNS/SQS y proveedor de credenciales en config/SqsTopologyConfig.java, separados de la lógica de negocio.
app.mensajeria.consumidores=true|false activa o desactiva los consumidores, útil para aislar problemas de AWS.
Disponibilidad con dos instancias
Varias instancias pueden leer la misma cola; SQS entrega cada mensaje a un consumidor a la vez y lo elimina solo tras el ACK. Si una instancia cae, sus mensajes vuelven a estar visibles al vencer el visibility timeout y otra instancia los toma.
# Instancia 1
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
# Instancia 2 (otra terminal)
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8083
Microservicio administrador (microservicio-admin, puerto 8081)
Toda la lógica de AWS está encapsulada en ColaAdminService; QueueAdminController solo delega.
Método
Endpoint
Descripción
POST
/api/admin/queues?name={nombre}
Crea una cola
DELETE
/api/admin/queues?url={queueUrl}
Elimina una cola
POST
/api/admin/topics?name={nombre}
Crea un tópico SNS
GET
/api/admin/queues/status?url={queueUrl}
Estado de una cola (mensajes disponibles, en vuelo, etc.)
POST
/api/admin/dlq/purge?url={queueUrl}
Purga los mensajes de una DLQ
Monitoreo y retención
Métricas de CloudWatch observadas sobre las colas:
Métrica
Qué indica
Decisión
Mensajes disponibles
Backlog esperando consumidor
Si crece, escalar consumidores
Mensajes en proceso
En vuelo, sin ACK
Si es alto y constante, subir el visibility timeout o buscar cuelgues
Antigüedad del mensaje más antiguo
Atraso de la cola
Si sube, hay un consumidor detenido o lento
Mensajes en DLQ
Fallos definitivos
Alarma: revisar, reprocesar o purgar
Alarma Alarma-Mensajes-Fallidos-DLQ: se activa cuando los mensajes visibles (ApproximateNumberOfMessagesVisible) en una DLQ son 1 o más. Un solo mensaje en la DLQ ya implica un fallo que requiere revisión.
Políticas de retención:
Notificaciones, 1 día: un aviso atrasado pierde valor.
Facturación, 4 días: dato crítico, con margen para recuperarse tras un fin de semana.
Inventario, 2 días: dato operativo; si se atrasa más, el stock queda desfasado.
DLQ, 14 días: tiempo para analizar y reprocesar; luego se purga con el administrador para evitar crecimiento descontrolado.
API de backend-pedidos
Todas las rutas /api/** exigen Authorization: Bearer <JWT> emitido por el User Pool de Cognito. Spring Security (OAuth2 Resource Server) valida firma, emisor y caducidad.
Método
Endpoint
Descripción
GET
/api/pedidos
Lista los pedidos
GET
/api/pedidos/{id}
Consulta un pedido
POST
/api/pedidos
Crea un pedido (publica PEDIDO_CREADO)
PUT
/api/pedidos/{id}/completar
Completa un pedido (publica PEDIDO_COMPLETADO)
DELETE
/api/pedidos/{id}
Elimina un pedido (publica PEDIDO_ELIMINADO)
GET
/api/productos
Lista los productos
GET
/api/productos/{id}
Consulta un producto
POST
/api/productos
Crea un producto
DELETE
/api/productos/{id}
Elimina un producto
Puesta en marcha local
Prerrequisitos
JDK 17 o superior y Maven (o el wrapper ./mvnw)
Node.js 18 o superior
Cuenta de AWS (Academy Learner Lab) con el tópico y las colas creados
Base de datos ArangoDB Cloud y User Pool de Cognito
Credenciales de AWS (no se suben al repositorio)
Las credenciales temporales del Learner Lab se cargan desde un archivo local ignorado por Git:
# backend-pedidos/src/main/resources/aws-credentials.properties  (en .gitignore)
aws.credentials.access-key=...
aws.credentials.secret-key=...
aws.credentials.session-token=...
application.properties lo importa con spring.config.import=optional:classpath:aws-credentials.properties. También se aceptan las variables aws_access_key_id, aws_secret_access_key y aws_session_token. El microservicio administrador usa la cadena de credenciales por defecto de AWS. Las credenciales del Learner Lab expiran con cada sesión y deben renovarse.
Ejecución
# 1. Backend de pedidos (puerto 8080)
cd backend-pedidos
./mvnw spring-boot:run

# 2. Microservicio administrador (puerto 8081)
cd microservicio-admin
mvn spring-boot:run

# 3. Frontend (puerto 5173)
cd frontend
npm install
npm run dev
El frontend inicia sesión con Cognito (configuración en frontend/src/aws-exports.js) y consume la API desde frontend/src/services/apiFetch.js, adjuntando el JWT en cada petición. CORS del backend está restringido al origen del frontend (http://localhost:5173).
Pruebas
cd backend-pedidos && ./mvnw test
cd microservicio-admin && mvn test
Integración de extremo a extremo
El usuario inicia sesión con Cognito en el frontend.
El frontend llama a la API con el JWT en el header Authorization: Bearer, a través de API Gateway.
El backend valida el JWT, guarda el pedido en ArangoDB y publica el evento en pedidos-topic.
SNS distribuye el evento a las colas según las Filter Policies, y cada consumidor lo procesa y confirma.
Si un mensaje falla de forma definitiva, termina en su DLQ y la alarma de CloudWatch avisa.
Buenas prácticas del repositorio
.gitignore por tecnología (Java/Maven, Node, IDEs, sistema operativo).
Las credenciales de AWS no se versionan.
Un solo repositorio con backend, microservicio administrador y frontend.
