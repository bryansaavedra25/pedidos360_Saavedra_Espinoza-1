import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Authenticator, useAuthenticator } from '@aws-amplify/ui-react';
import Dashboard from './components/Dashboard';

// Guard para proteger rutas privadas
function RutaProtegida({ children }) {
  const { authStatus } = useAuthenticator((context) => [context.authStatus]);

  if (authStatus === 'configuring') {
    return <div>Cargando...</div>;
  }

  if (authStatus !== 'authenticated') {
    return <Navigate to="/login" replace />;
  }

  return children;
}

export default function App() {
  return (
    <Authenticator.Provider>
      <BrowserRouter>
        <Routes>
          {/* Ruta pública para el Login usando Authenticator de Amplify */}
          <Route
            path="/login"
            element={
              <div style={{ display: 'flex', justifyContent: 'center', marginTop: '50px' }}>
                <Authenticator>
                  {() => <Navigate to="/dashboard" replace />}
                </Authenticator>
              </div>
            }
          />

          {/* Ruta Protegida */}
          <Route
            path="/dashboard"
            element={
              <RutaProtegida>
                <Dashboard />
              </RutaProtegida>
            }
          />

          {/* Redirección por defecto */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </Authenticator.Provider>
  );
}