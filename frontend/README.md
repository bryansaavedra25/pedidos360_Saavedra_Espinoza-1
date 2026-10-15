# 💻 Pedidos360 - Frontend React

Aplicación SPA (Single Page Application) desarrollada en **React + Vite**, que sirve como panel de administración (Dashboard) para el sistema **Pedidos360**. 

El proyecto integra autenticación gestionada por **AWS Cognito (Amplify)** y consume la API REST protegida del backend alojada en una instancia **AWS EC2**.

Este desarrollo corresponde a la Evaluación Parcial N° 1 de la asignatura **Desarrollo Cloud Native I (DSY1107)** de Duoc UC.

---

## 🏛️ Arquitectura e Integración

[ Usuario ] ──> [ React SPA + Amplify UI ]
│
├──> (Autenticación) ──> [ AWS Cognito User Pool ]
│
└──> (HTTP Bearer JWT) ──> [ AWS EC2 API (Spring Boot) ]


* **Autenticación IDaaS:** Gestión completa del flujo de Login/Logout, refresco de sesión y obtención de tokens JWT mediante `@aws-amplify/auth`.
* **Consumo de API Protegida:** Envío automático del `Authorization: Bearer <ID_TOKEN>` en cada petición HTTP hacia el backend remoto.
* **Interfaz Dinámica:** Formulario interactivo para registrar/editar pedidos y listado en tiempo real con capacidades CRUD.

---

## 🛠️ Tecnologías Utilizadas

* **React 18** (Librería UI)
* **Vite** (Build tool y servidor de desarrollo)
* **AWS Amplify SDK** (`aws-amplify` & `@aws-amplify/ui-react`)
* **JavaScript (ES6+) / CSS3**

---

## ⚙️ Variables de Entorno (`.env`)

Para ejecutar la aplicación localmente o en un entorno de pruebas, se debe crear un archivo `.env` en la raíz del proyecto con las siguientes variables:

```env
VITE_COGNITO_USER_POOL_ID=us-east-1_dzuH5paum
VITE_COGNITO_CLIENT_ID=59ukm8nie3gsecfi1se8dshhmm6
VITE_API_URL=[http://54.198.41.234:8080/api/pedidos](http://54.198.41.234:8080/api/pedidos)
⚠️ Nota de Seguridad: El archivo .env se encuentra explícitamente excluido del repositorio en .gitignore para prevenir la divulgación de credenciales en el control de versiones.

🚀 Instalación y Ejecución Local
1. Clonar el repositorio
Bash
git clone [https://github.com/EAikeE/frontend-pedidos360.git](https://github.com/EAikeE/frontend-pedidos360.git)
cd frontend-pedidos360
2. Instalar dependencias
Bash
npm install
3. Crear el archivo de configuración .env
Crea el archivo .env en la raíz con las variables indicadas en la sección anterior.

4. Iniciar el servidor de desarrollo
Bash
npm run dev
La aplicación estará disponible localmente en http://localhost:5173.

👨‍💻 Autores
Bryan Saavedra, Elena espinoza
Asignatura: Desarrollo Cloud Native I 
Duoc UC - 20256
Asignatura: Desarrollo Cloud Native I (DSY1107)

Duoc UC - 2025
