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
  { label: preferences.t('admin.menuComments'), to: '/admin/comments' },
  { label: preferences.t('admin.menuRagFeedback'), to: '/admin/rag-feedback' },
  { label: preferences.locale === 'zh-CN' ? 'Agent 运维' : 'Agent Ops', to: '/admin/agents' },
  { label: preferences.locale === 'zh-CN' ? 'Agent 工具调用' : 'Agent Tool Calls', to: '/admin/agent-tool-calls' }
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
        <div class="sidebar-top">
          <div class="sidebar-heading">
            <span class="console-kicker">Console</span>
            <h1 class="console-title">{{ preferences.t('admin.consoleTitle') }}</h1>
          </div>
          <div class="sidebar-controls">
            <AppControls />
          </div>
        </div>

        <div class="brand-card">
          <span class="admin-eyebrow">Workspace</span>
          <h2>{{ preferences.t('admin.blogAdmin') }}</h2>
          <p class="muted">
            {{ preferences.t('admin.operator') }}:
            {{ authStore.user?.displayName || authStore.user?.username }}
          </p>
        </div>
      </div>

      <nav class="menu">
        <router-link v-for="menu in menus" :key="menu.to" :to="menu.to">
          <span>{{ menu.label }}</span>
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
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-height: 0;
  height: 100vh;
  padding: 28px 24px;
  color: var(--text-main);
  border-right: 1px solid var(--line);
  background: var(--admin-sidebar-bg);
  overflow: auto;
}

.sidebar-top {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
}

.sidebar-heading {
  min-width: 0;
}

.sidebar-controls {
  align-self: flex-start;
}

.brand-block {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.brand-card {
  padding: 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: var(--admin-soft-bg);
}

.brand-card h2 {
  margin: 0;
  font-size: 1.55rem;
  letter-spacing: -0.04em;
  text-transform: none;
}

.brand-card p {
  margin: 10px 0 0;
  line-height: 1.7;
}

.console-kicker {
  display: inline-flex;
  margin-bottom: 10px;
  color: var(--text-secondary);
  letter-spacing: 0.14em;
  font-size: 0.76rem;
}

.console-title {
  margin: 0;
  font-size: clamp(2rem, 4vw, 2.6rem);
  letter-spacing: -0.05em;
  text-transform: none;
  color: var(--text-main);
}

.menu,
.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.menu {
  flex: 1;
}

.menu a,
.sidebar-footer a,
.sidebar-footer button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 50px;
  padding: 13px 15px;
  border: 1px solid var(--line);
  border-radius: 16px;
  color: var(--text-secondary);
  background: var(--admin-soft-bg);
  transition: 0.2s ease;
}

.menu a:hover,
.sidebar-footer a:hover,
.sidebar-footer button:hover,
.menu .router-link-active {
  color: var(--text-main);
  background: var(--admin-soft-hover);
  border-color: var(--line-strong);
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(44, 28, 10, 0.08);
}

button {
  font: inherit;
  text-align: left;
  cursor: pointer;
}

@media (max-width: 960px) {
  .sidebar {
    position: relative;
    height: auto;
    min-height: auto;
    border-right: none;
    border-bottom: 1px solid var(--line);
    overflow: visible;
  }

  .sidebar-top {
    flex-direction: column;
  }
}
</style>
