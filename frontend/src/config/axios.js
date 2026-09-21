import axios from "axios";

// Creamos una instancia de Axios para configurar la URL base de tu backend
const clienteAxios = axios.create({
  baseURL: import.meta.env.VITE_BACKEND_URL || "http://localhost:5000/api",
});

// Interceptor para inyectar el token JWT en las peticiones que lo requieran
clienteAxios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

// Interceptor para manejar errores globalmente
clienteAxios.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Si el error es 401 (Unauthorized) o 403 (Forbidden), el token caducó o la cuenta fue desactivada
    if (
      error.response &&
      (error.response.status === 401 || error.response.status === 403)
    ) {
      localStorage.removeItem("token");
      // Opcional: Redirigir al usuario al login manualmente, aunque el AuthProvider debería detectarlo
      if (
        window.location.pathname !== "/" &&
        window.location.pathname !== "/registrar" &&
        window.location.pathname !== "/confirmar"
      ) {
        window.location.href = "/";
      }
    }
    return Promise.reject(error);
  },
);

export default clienteAxios;
