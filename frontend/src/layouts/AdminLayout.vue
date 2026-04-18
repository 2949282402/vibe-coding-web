<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import AppControls from '../components/AppControls.vue';
import { useAuthStore } from '../stores/auth';
import { usePreferencesStore } from '../stores/preferences';

const authStore = useAuthStore();
const preferences = usePreferencesStore();
const router = useRouter();

const menus = computed(() => [
  { label: preferences.t('admin.menuOverview'), to: '/admin/dashboard' },
  { label: preferences.t('admin.menuPosts'), to: '/admin/posts' },
  { label: preferences.t('admin.menuTaxonomy'), to: '/admin/taxonomies' },
  { label: preferences.t('admin.menuComments'), to: '/admin/comments' }
]);

const logout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<template>
  <div class="admin-grid">
    <aside class="sidebar">
      <div class="brand-block">
        <h1 class="console-title">{{ preferences.t('admin.consoleTitle') }}</h1>
        <div class="sidebar-top">
          <AppControls />
        </div>
        <h2>{{ preferences.t('admin.blogAdmin') }}</h2>
        <p class="muted">
          {{ preferences.t('admin.operator') }}:
          {{ authStore.user?.displayName || authStore.user?.username }}
        </p>
      </div>

      <nav class="menu">
        <router-link v-for="menu in menus" :key="menu.to" :to="menu.to">
          {{ menu.label }}
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/">{{ preferences.t('admin.backToSite') }}</router-link>
        <button type="button" @click="logout">{{ preferences.t('admin.signOut') }}</button>
      </div>
    </aside>

    <section class="admin-main">
      <router-view />
    </section>
  </div>
</template>

<style scoped>
.sidebar {
  min-height: 100vh;
  padding: 28px 24px;
  color: var(--text-main);
  border-right: 1px solid var(--line);
  background: var(--admin-sidebar-bg);
}

.sidebar-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.brand-block h2 {
  margin: 14px 0 10px;
  font-size: 2rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.console-title {
  margin: 0 0 16px;
  font-size: 2.4rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-main);
}

.menu,
.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 26px;
}

.menu a,
.sidebar-footer a,
.sidebar-footer button {
  padding: 13px 15px;
  border: 1px solid var(--line);
  border-radius: 14px;
  color: var(--text-secondary);
  background: var(--bg-panel);
  transition: 0.2s ease;
}

.menu a:hover,
.sidebar-footer a:hover,
.sidebar-footer button:hover,
.menu .router-link-active {
  color: var(--text-main);
  background: var(--bg-panel-hover);
  border-color: var(--line-strong);
}

button {
  font: inherit;
  text-align: left;
  cursor: pointer;
}

@media (max-width: 960px) {
  .sidebar-top {
    flex-direction: column;
  }
}
</style>
