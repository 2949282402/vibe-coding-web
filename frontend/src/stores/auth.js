import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

const TOKEN_KEY = 'blog-auth-token';
const USER_KEY = 'blog-auth-user';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || localStorage.getItem('blog-admin-token') || '');
  const user = ref(JSON.parse(localStorage.getItem(USER_KEY) || localStorage.getItem('blog-admin-user') || 'null'));

  const isAuthenticated = computed(() => Boolean(token.value));
  const isAdmin = computed(() => user.value?.role === 'ADMIN');

  const setSession = (payload) => {
    token.value = payload.token;
    user.value = payload.user;
    localStorage.setItem(TOKEN_KEY, payload.token);
    localStorage.setItem(USER_KEY, JSON.stringify(payload.user));
    localStorage.removeItem('blog-admin-token');
    localStorage.removeItem('blog-admin-user');
  };

  const logout = () => {
    token.value = '';
    user.value = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem('blog-admin-token');
    localStorage.removeItem('blog-admin-user');
  };

  return { token, user, isAuthenticated, isAdmin, setSession, logout };
});
