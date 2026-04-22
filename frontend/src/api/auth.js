import http from './http';

export const loginApi = (payload) => http.post('/auth/login', payload);
export const registerApi = (payload) => http.post('/auth/register', payload);
export const fetchMeApi = () => http.get('/auth/me');
export const fetchQwenConfigApi = () => http.get('/auth/qwen-config', { timeout: 30000 });
export const saveQwenConfigApi = (payload) => http.post('/auth/qwen-config', payload, { timeout: 90000 });
