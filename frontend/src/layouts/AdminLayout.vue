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
  { label: preferences.locale === 'zh-CN' ? 'Agent 草稿审核' : 'Agent Draft Review', to: '/admin/agent-drafts' },
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
            <p class="console-copy muted">{{ preferences.t('admin.blogAdmin') }}</p>
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

      <nav class="menu admin-surface">
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
  gap: 18px;
  min-height: 0;
  height: 100vh;
  padding: 24px 20px 20px;
  color: var(--text-main);
  border-right: 1px solid var(--line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015)),
    var(--admin-sidebar-bg);
  overflow: auto;
  backdrop-filter: blur(34px) saturate(140%);
  -webkit-backdrop-filter: blur(34px) saturate(140%);
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.04);
}

.sidebar-top {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
}

.sidebar-heading {
  min-width: 0;
}

.console-copy {
  margin: 8px 0 0;
  line-height: 1.6;
  max-width: 240px;
}

.sidebar-controls {
  align-self: flex-start;
}

.brand-block {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.brand-card {
  padding: 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.04)),
    rgba(255, 255, 255, 0.03);
  box-shadow: inset 0 1px 0 var(--glow-soft), 0 20px 38px rgba(0, 0, 0, 0.16);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
}

.brand-card h2 {
  margin: 0;
  font-size: 1.36rem;
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
  color: var(--text-muted);
  letter-spacing: 0.18em;
  font-size: 0.7rem;
  text-transform: uppercase;
}

.console-title {
  margin: 0;
  font-size: clamp(2rem, 4vw, 2.4rem);
  letter-spacing: -0.05em;
  text-transform: none;
  color: var(--text-main);
}

.menu,
.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.menu {
  flex: 1;
  padding: 10px;
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.03)),
    rgba(255, 255, 255, 0.02);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
}

.menu a,
.sidebar-footer a,
.sidebar-footer button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0);
  border-radius: 16px;
  color: var(--text-secondary);
  background: transparent;
  transition: background-color 0.28s var(--ease-soft), color 0.28s var(--ease-soft), border-color 0.28s var(--ease-soft), transform 0.28s var(--ease-liquid), box-shadow 0.28s var(--ease-liquid);
}

.menu a:hover,
.sidebar-footer a:hover,
.sidebar-footer button:hover,
.menu .router-link-active {
  color: var(--text-main);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.05)),
    rgba(255, 255, 255, 0.03);
  border-color: rgba(255, 255, 255, 0.14);
  box-shadow: 0 16px 28px rgba(0, 0, 0, 0.14);
  transform: translateY(-2px);
}

button {
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.sidebar-footer {
  padding: 10px;
  border-radius: 22px;
  border: 1px solid var(--line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.02)),
    rgba(255, 255, 255, 0.02);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.admin-main {
  background: var(--admin-shell-bg);
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
