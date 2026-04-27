<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppControls from '../components/AppControls.vue';
import { useAuthStore } from '../stores/auth';
import { usePreferencesStore } from '../stores/preferences';

const preferences = usePreferencesStore();
const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const immersiveMode = computed(() => Boolean(route.meta.immersive));
const hideFooter = computed(() => Boolean(route.meta.hideFooter));

const authCopy = computed(() =>
  preferences.locale === 'zh-CN'
    ? {
        login: '登录',
        logout: '退出登录',
        admin: '后台',
        profile: '当前用户'
      }
    : {
        login: 'Sign In',
        logout: 'Sign Out',
        admin: 'Admin',
        profile: 'Current User'
      }
);

const navItems = computed(() => {
  const items = [
    { label: preferences.t('main.navHome'), to: '/' },
    { label: preferences.t('main.navKnowledge'), to: '/knowledge' },
    { label: preferences.t('main.navArchive'), to: '/archives' },
    { label: preferences.t('main.navCategories'), to: '/categories' }
  ];
  if (authStore.isAdmin) {
    items.push({ label: authCopy.value.admin, to: '/admin' });
  }
  return items;
});

function isNavActive(item) {
  if (item.to === '/') {
    return route.path === '/';
  }
  return route.path === item.to || route.path.startsWith(`${item.to}/`);
}

function goLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } });
}

function logout() {
  authStore.logout();
  router.push('/');
}
</script>

<template>
  <div class="page-shell" :class="{ 'page-shell-immersive': immersiveMode }">
    <header class="topbar glass-panel" :class="{ 'topbar-immersive': immersiveMode }">
      <div class="brand-shell">
        <router-link to="/" class="brand">
          <span class="mark">HJ</span>
          <div>
            <strong>HeJulian Blog</strong>
            <p>{{ preferences.t('main.brandSubtitle') }}</p>
          </div>
        </router-link>
      </div>

      <div class="topbar-actions">
        <nav class="nav glass-subnav">
          <router-link
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            :class="{ active: isNavActive(item) }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div v-if="authStore.isAuthenticated" class="user-rail glass-subnav">
          <div class="user-meta">
            <span class="user-label">{{ authCopy.profile }}</span>
            <strong>{{ authStore.user?.displayName || authStore.user?.username }}</strong>
          </div>
          <button type="button" class="auth-btn" @click="logout">{{ authCopy.logout }}</button>
        </div>
        <button v-else type="button" class="auth-btn glass-subnav" @click="goLogin">{{ authCopy.login }}</button>

        <AppControls />
      </div>
    </header>

    <main :class="{ 'main-immersive': immersiveMode }">
      <router-view />
    </main>

    <footer v-if="!hideFooter" class="footer muted">
      <span>{{ preferences.t('main.footerProduct') }}</span>
      <span>{{ preferences.t('main.footerOps') }}</span>
    </footer>
  </div>
</template>

<style scoped>
.page-shell-immersive {
  min-height: 100vh;
  height: 100vh;
  padding: 12px 12px 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.topbar {
  position: relative;
  z-index: 12;
  margin-bottom: 28px;
  padding: 14px 18px;
  border-radius: var(--radius-xl);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  background: rgba(18, 19, 22, 0.72);
}

html[data-theme='light'] .topbar {
  background: rgba(255, 255, 255, 0.9);
}

.topbar::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  border: 1px solid rgba(255, 255, 255, 0.025);
}

html[data-theme='light'] .topbar::after {
  border-color: rgba(0, 0, 0, 0.035);
}

.topbar-immersive {
  position: relative;
  z-index: 12;
  margin-bottom: 12px;
  border-radius: 20px;
  backdrop-filter: blur(14px);
}

.brand-shell {
  min-width: 0;
}

.brand {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.brand strong {
  display: block;
  font-size: 0.98rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.brand p,
.footer {
  margin: 0;
}

.brand p {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.mark {
  width: 48px;
  height: 48px;
  border-radius: 15px;
  display: grid;
  place-items: center;
  font-weight: 800;
  letter-spacing: 0.16em;
  color: #0d0d0f;
  background: linear-gradient(180deg, #f5f5f6, #d8d8dd);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.14);
}

html[data-theme='light'] .mark {
  color: #ffffff;
  background: linear-gradient(180deg, #111214, #35363b);
  border-color: rgba(0, 0, 0, 0.08);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.08);
}

.topbar-actions {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.glass-subnav {
  padding: 5px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.025);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.02);
}

html[data-theme='light'] .glass-subnav {
  background: rgba(17, 17, 17, 0.03);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.nav {
  position: relative;
  z-index: 3;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.nav a {
  position: relative;
  z-index: 3;
  padding: 10px 15px;
  border-radius: 999px;
  border: 1px solid transparent;
  color: var(--text-secondary);
  transition: background-color 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.nav a:hover,
.nav a.active {
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.06);
  border-color: var(--line-strong);
}

.user-rail {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding-inline: 12px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-label {
  font-size: 0.72rem;
  color: var(--text-muted);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.auth-btn {
  border: 0;
  color: var(--text-main);
  background: transparent;
  cursor: pointer;
  min-height: 42px;
  padding: 0 16px;
  border-radius: 999px;
  font: inherit;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.auth-btn:hover {
  background: rgba(255, 255, 255, 0.06);
}

.footer {
  padding: 30px 4px 0;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  color: var(--text-muted);
  text-transform: uppercase;
}

.main-immersive {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

@media (max-width: 960px) {
  .topbar {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .topbar-actions {
    width: 100%;
    justify-content: space-between;
  }

  .glass-subnav {
    width: 100%;
    border-radius: 24px;
  }
}

@media (max-width: 720px) {
  .page-shell-immersive {
    padding: 8px;
  }

  .topbar-immersive {
    margin-bottom: 8px;
  }

  .topbar-actions,
  .footer,
  .user-rail {
    flex-direction: column;
    align-items: stretch;
  }

  .brand,
  .glass-subnav,
  .nav {
    width: 100%;
  }

  .nav a,
  .auth-btn {
    flex: 1 1 calc(50% - 8px);
    justify-content: center;
    text-align: center;
  }
}
</style>
