import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('blog-admin-token') || '');
  const user = ref(JSON.parse(localStorage.getItem('blog-admin-user') || 'null'));

  const isAuthenticated = computed(() => Boolean(token.value));

  const setSession = (payload) => {
    token.value = payload.token;
    user.value = payload.user;
    localStorage.setItem('blog-admin-token', payload.token);
    localStorage.setItem('blog-admin-user', JSON.stringify(payload.user));
  };

  const logout = () => {
    token.value = '';
    user.value = null;
    localStorage.removeItem('blog-admin-token');
    localStorage.removeItem('blog-admin-user');
  };

  return { token, user, isAuthenticated, setSession, logout };
});

