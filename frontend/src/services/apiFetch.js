import { fetchAuthSession } from 'aws-amplify/auth';

const API_BASE_URL = 'http://localhost:8080/api'; // Ruta base de la API Backend (Spring Boot / API Gateway)

/**
 * Petición HTTP personalizada con inyección de Token Bearer JWT de Cognito
 */
export async function apiFetch(endpoint, options = {}) {
  try {
    const session = await fetchAuthSession(); // Obtiene la sesión actual desde Cognito
    const token = session.tokens?.idToken?.toString() || session.tokens?.accessToken?.toString();

    if (!token) {
      throw new Error('No se encontró un token válido. Inicie sesión.');
    }

    // Cabeceras con Token de Autorización
    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      ...options.headers,
    };

    // Realiza la petición HTTP
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      // Intenta obtener detalles del error enviados por el servidor
      const errorText = await response.text();
      throw new Error(errorText || `Error ${response.status}: ${response.statusText}`);
    }

    // 1. Manejo de respuesta 204 (No Content) típica en peticiones DELETE
    if (response.status === 204) {
      return null;
    }

    // 2. Lee la respuesta como texto para verificar si viene vacía antes de parsear JSON
    const text = await response.text();
    return text ? JSON.parse(text) : null;

  } catch (error) {
    console.error('Error en interceptor apiFetch:', error);
    throw error;
  }
}

/**
 * Extrae los Claims del IdToken de Cognito (Email, Username y Roles/Grupos)
 */
export async function getUserClaims() {
  try {
    const session = await fetchAuthSession();
    const payload = session.tokens?.idToken?.payload;

    if (!payload) return null;

    // Extrae los grupos de Cognito (cognito:groups)
    const rawGroups = payload['cognito:groups'];
    const roles = Array.isArray(rawGroups) ? rawGroups : (rawGroups ? [rawGroups] : []);

    return {
      username: payload['cognito:username'] || payload.email || 'Usuario',
      email: payload.email || 'Sin email registrado',
      roles: roles, // Arreglo con los grupos de Cognito asignados
      claimsCompletos: payload
    };
  } catch (error) {
    console.error('Error al obtener claims:', error);
    return null;
  }
}