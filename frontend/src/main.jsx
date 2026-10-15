import React from 'react';
import ReactDOM from 'react-dom/client';
import { Amplify } from 'aws-amplify';
import awsconfig from './aws-exports';
import App from './App.jsx';

import '@aws-amplify/ui-react/styles.css'; //Importar los estilos visuales del componente de login de Amplify

Amplify.configure(awsconfig);// Carga la configuración de AWS Amplify

ReactDOM.createRoot(document.getElementById('root')).render( // Monta la aplicación React en el contenedor del DOM
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
