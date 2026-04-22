import axios from 'axios';
import { ElMessage } from 'element-plus';

const http = axios.create({
  baseURL: '/api',
  timeout: 12000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('blog-auth-token') || localStorage.getItem('blog-admin-token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message = error.response?.data?.message || error.message || 'Request failed';
    if (error.response?.status === 401) {
      localStorage.removeItem('blog-auth-token');
      localStorage.removeItem('blog-auth-user');
      localStorage.removeItem('blog-admin-token');
      localStorage.removeItem('blog-admin-user');
      if (location.pathname.startsWith('/admin') || location.pathname.startsWith('/knowledge')) {
        location.href = '/login';
      }
    } else {
      ElMessage.error(message);
    }
    return Promise.reject(error);
  }
);

export default http;
