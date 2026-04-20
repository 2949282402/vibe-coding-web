import http from './http';

export const fetchDashboardApi = () => http.get('/admin/dashboard');
export const fetchAdminPostsApi = (params) => http.get('/admin/posts', { params });
export const fetchAdminPostApi = (id) => http.get(`/admin/posts/${id}`);
export const saveAdminPostApi = (payload) =>
  payload.id ? http.put(`/admin/posts/${payload.id}`, payload) : http.post('/admin/posts', payload);
export const deleteAdminPostApi = (id) => http.delete(`/admin/posts/${id}`);
export const uploadAdminImageApi = (formData) =>
  http.post('/admin/uploads/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });

export const fetchCategoriesApi = () => http.get('/admin/categories');
export const saveCategoryApi = (payload) =>
  payload.id ? http.put(`/admin/categories/${payload.id}`, payload) : http.post('/admin/categories', payload);
export const deleteCategoryApi = (id) => http.delete(`/admin/categories/${id}`);

export const fetchTagsApi = () => http.get('/admin/tags');
export const saveTagApi = (payload) =>
  payload.id ? http.put(`/admin/tags/${payload.id}`, payload) : http.post('/admin/tags', payload);
export const deleteTagApi = (id) => http.delete(`/admin/tags/${id}`);

export const fetchAdminCommentsApi = () => http.get('/admin/comments');
export const reviewCommentApi = (id, status) => http.put(`/admin/comments/${id}/status`, { status });
export const fetchAdminRagFeedbackApi = (params) => http.get('/admin/rag-feedback', { params });
export const exportAdminRagFeedbackCsvApi = (params) =>
  http.get('/admin/rag-feedback/export', { params, responseType: 'blob' });
