

Microservicio RESTful para la plataforma **Pedidos360**, desarrollado con **Java 21** y **Spring Boot 3**. Diseñado bajo una arquitectura *Cloud Native*, ofrece servicios de gestión de pedidos con seguridad integrada mediante tokens JWT (*AWS Cognito*) y persistencia flexible NoSQL (*ArangoDB Cloud*).

---

## 🛠️ Tecnologías Utilizadas

* **Lenguaje:** Java 21
* **Framework:** Spring Boot 3.3.4
* **Seguridad:** Spring Security (OAuth2 Resource Server) + AWS Cognito JWT
* **Persistencia:** ArangoDB Cloud (Base de datos NoSQL basada en documentos JSON)
* **Gestor de Dependencias:** Maven (Wrapper incluido)
* **Despliegue:** Amazon Web Services (AWS EC2 / API Gateway)[cite: 8, 11, 12]

---

## 🔒 Arquitectura de Seguridad

El backend actúa como un **OAuth2 Resource Server** que delega la autenticación a **AWS Cognito**:
1. Todas las peticiones dirigidas a la API `/api/**` deben incluir el encabezado `Authorization: Bearer <JWT>`[cite: 8].
2. **Spring Security** valida de forma automática la firma digital criptográfica, el emisor (*issuer*) y la caducidad del token emitido por el User Pool de Cognito[cite: 8].
3. Configuración de **CORS** (`CorsConfig.java`) restringida únicamente al origen del cliente frontend (`http://localhost:5173`)[cite: 8].

---

## 📁 Estructura del Proyecto

```text
src/main/java/com/pedidos360/backend/
├── config/             # Configuración de ArangoDB, CORS y Spring Security
├── controller/         # Controladores REST para exponer la API de pedidos
├── model/              # Entidades NoSQL (@Document)
├── repository/         # Interfaces de persistencia (arangodb-spring-data)
├── service/            # Lógica de negocio de la aplicación
└── BackendApplication.java
🚀 Configuración y Ejecución LocalPrerrequisitosJDK 21 instalado.Maven (o utilizar ./mvnw incluido).   Instancia/Clúster de ArangoDB Cloud activo[cite: 7, 8].User Pool de AWS Cognito configurado[cite: 8].Variables de Entorno / application.propertiesConfigura las credenciales en el archivo src/main/resources/application.properties:Properties# Puerto de la aplicación
server.port=8080

# Conexión ArangoDB Cloud
arangodb.host=your-arangodb-host.arangodb.cloud
arangodb.port=8529
arangodb.user=root
arangodb.password=your-password
arangodb.database=_system

# AWS Cognito (OAuth2 Resource Server)
spring.security.oauth2.resourceserver.jwt.issuer-uri=[https://cognito-idp.us-east-1.amazonaws.com/your-user-pool-id](https://cognito-idp.us-east-1.amazonaws.com/your-user-pool-id)
Comandos de EjecuciónClonar el repositorio:Bashgit clone [https://github.com/bryansaavedra25/backend_pedidos360_Saavedra_Espinoza.git](https://github.com/bryansaavedra25/backend_pedidos360_Saavedra_Espinoza.git)
cd backend_pedidos360_Saavedra_Espinoza
Compilar el proyecto:Bash./mvnw clean package
Iniciar la aplicación:Bash./mvnw spring-boot:run
🌐 Endpoints Principales API RESTMétodoEndpointDescripciónAutenticaciónGET/api/pedidosObtener listado completo de pedidos🔐 Requiere JWTPOST/api/pedidosCrear un nuevo pedido🔐 Requiere JWTGET/api/pedidos/{id}Consultar pedido por ID🔐 Requiere JWTDELETE/api/pedidos/{id}Eliminar un pedido🔐 Requiere JWT👥 Autores y ColaboradoresBryan Saavedra (@bryansaavedra25)[cite: 14]Elena Espinoza (@EAikeE)[cite: 14]Proyecto desarrollado para la asignatura de Desarrollo Cloud Native I — Duoc UC.
