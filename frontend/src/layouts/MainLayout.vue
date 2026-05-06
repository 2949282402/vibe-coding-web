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
        profile: '当前用户',
        issue: '内容现场',
        desk: '个人知识与内容系统'
      }
    : {
        login: 'Sign In',
        logout: 'Sign Out',
        admin: 'Admin',
        profile: 'Current User',
        issue: 'Current Issue',
        desk: 'Personal knowledge and publishing system'
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
      <div class="topbar-brand-layer">
        <router-link to="/" class="brand">
          <span class="mark">HJ</span>
          <div class="brand-copy">
            <span class="topbar-label">{{ authCopy.issue }}</span>
            <strong class="topbar-title">HeJulian Blog</strong>
            <p class="topbar-subtitle">{{ authCopy.desk }}</p>
          </div>
        </router-link>
      </div>

      <div class="topbar-nav-layer">
        <nav class="nav editorial-nav">
          <router-link
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            :class="{ active: isNavActive(item) }"
          >
            <span>{{ item.label }}</span>
          </router-link>
        </nav>

        <div class="topbar-actions">
          <div class="topbar-utility">
            <div v-if="authStore.isAuthenticated" class="user-rail">
              <div class="user-meta">
                <span class="user-label">{{ authCopy.profile }}</span>
                <strong>{{ authStore.user?.displayName || authStore.user?.username }}</strong>
              </div>
              <button type="button" class="auth-btn auth-btn-secondary" @click="logout">{{ authCopy.logout }}</button>
            </div>
            <button v-else type="button" class="auth-btn auth-btn-primary" @click="goLogin">{{ authCopy.login }}</button>

            <div class="controls-wrap">
              <AppControls />
            </div>
          </div>
        </div>
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
  padding: 6px 12px 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.topbar {
  position: sticky;
  top: 14px;
  z-index: 12;
  margin-bottom: 20px;
  padding: 18px 22px 16px;
  border-radius: 28px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: flex-start;
  gap: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.03)),
    linear-gradient(180deg, rgba(16, 16, 16, 0.76), rgba(16, 16, 16, 0.48)),
    rgba(13, 17, 24, 0.56);
  backdrop-filter: blur(30px) saturate(140%);
  -webkit-backdrop-filter: blur(30px) saturate(140%);
  box-shadow: var(--glass-edge), 0 24px 54px rgba(0, 0, 0, 0.24);
  animation: topbar-float-in 0.72s var(--ease-soft);
  transition: transform 0.34s var(--ease-liquid), box-shadow 0.34s var(--ease-liquid), background 0.34s var(--ease-soft);
}

html[data-theme='light'] .topbar {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.58), rgba(255, 255, 255, 0.26)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(243, 243, 243, 0.8)),
    rgba(255, 255, 255, 0.48);
}

.topbar::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), transparent 22%),
    radial-gradient(circle at 18% 0%, rgba(255, 255, 255, 0.08), transparent 22%);
}

html[data-theme='light'] .topbar::after {
  border-color: rgba(0, 0, 0, 0.035);
}

.topbar-immersive {
  position: relative;
  top: auto;
  margin-bottom: 12px;
  border-radius: 20px;
}

.topbar-brand-layer,
.topbar-nav-layer,
.brand,
.topbar-utility {
  position: relative;
  z-index: 2;
}

.topbar-brand-layer,
.topbar-nav-layer {
  min-width: 0;
}

.brand {
  display: inline-flex;
  align-items: flex-start;
  gap: 14px;
  min-width: 0;
}

.brand-copy {
  display: grid;
  gap: 0;
}

.topbar-label,
.user-label {
  font-size: 0.72rem;
  color: var(--text-muted);
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.topbar-label {
  margin-bottom: 6px;
}

.topbar-title {
  display: block;
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.28rem;
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.brand p,
.footer {
  margin: 0;
}

.topbar-subtitle {
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.6;
  margin-top: 6px;
  max-width: 640px;
}

.mark {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  font-weight: 800;
  letter-spacing: 0.16em;
  color: #0d0d0f;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(170, 173, 180, 0.82));
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.34), 0 14px 28px rgba(0, 0, 0, 0.2);
}

html[data-theme='light'] .mark {
  color: #ffffff;
  background: linear-gradient(180deg, #111214, #35363b);
  border-color: rgba(0, 0, 0, 0.08);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.08);
}

.topbar-nav-layer {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 14px;
}

.editorial-nav {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  padding: 5px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.03)),
    rgba(255, 255, 255, 0.02);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: var(--glass-edge), 0 10px 24px rgba(0, 0, 0, 0.08);
  flex: 0 1 auto;
  min-width: 0;
}

html[data-theme='light'] .editorial-nav {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(243, 243, 243, 0.8)),
    rgba(17, 17, 17, 0.03);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.nav a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
  transition: background-color 0.28s var(--ease-soft), color 0.28s var(--ease-soft), border-color 0.28s var(--ease-soft), transform 0.28s var(--ease-liquid), box-shadow 0.28s var(--ease-liquid);
}

.nav a:hover,
.nav a.active {
  color: var(--text-main);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.05)),
    rgba(255, 255, 255, 0.03);
  border-color: rgba(255, 255, 255, 0.14);
  transform: translateY(-1px);
  box-shadow: var(--glass-edge), 0 10px 20px rgba(0, 0, 0, 0.1);
}

.topbar-actions,
.topbar-utility {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
}

.topbar-actions {
  flex: 1 1 auto;
  min-width: 0;
}

.topbar-utility {
  gap: 12px;
  flex: 0 0 auto;
  flex-wrap: wrap;
  padding-left: 2px;
}

.user-rail {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-height: 42px;
  padding: 6px 8px 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.03)),
    rgba(255, 255, 255, 0.02);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: var(--glass-edge), 0 10px 24px rgba(0, 0, 0, 0.08);
}

html[data-theme='light'] .user-rail {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(243, 243, 243, 0.8)),
    rgba(17, 17, 17, 0.03);
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.auth-btn {
  border: 0;
  cursor: pointer;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  font: inherit;
  font-size: 0.78rem;
  font-weight: 600;
  transition: transform 0.28s var(--ease-liquid), background-color 0.28s var(--ease-soft), color 0.28s var(--ease-soft), box-shadow 0.28s var(--ease-liquid), border-color 0.28s var(--ease-soft);
}

.auth-btn:hover {
  transform: translateY(-2px);
}

.auth-btn-primary {
  color: #171412;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(191, 191, 191, 0.82));
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.14);
}

html[data-theme='light'] .auth-btn-primary {
  color: #ffffff;
  background: linear-gradient(135deg, rgba(29, 29, 31, 0.95), rgba(68, 68, 74, 0.94));
  border-color: rgba(20, 20, 20, 0.14);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.08);
}

.auth-btn-secondary {
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.controls-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.footer {
  padding: 36px 8px 0;
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

@keyframes topbar-float-in {
  0% {
    opacity: 0;
    transform: translateY(-14px) scale(0.985);
  }

  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 960px) {
  .topbar {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .topbar-nav-layer {
    display: grid;
    gap: 14px;
    justify-content: stretch;
  }

  .topbar-actions,
  .editorial-nav {
    justify-content: flex-start;
  }

  .topbar-utility {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}

@media (max-width: 720px) {
  .page-shell-immersive {
    padding: 4px 8px 8px;
  }

  .topbar-immersive {
    margin-bottom: 6px;
  }

  .topbar {
    padding: 14px;
    gap: 14px;
  }

  .editorial-nav,
  .user-rail,
  .topbar-utility,
  .footer {
    border-radius: 22px;
  }

  .topbar-utility,
  .footer,
  .user-rail {
    flex-direction: column;
    align-items: stretch;
  }

  .brand,
  .editorial-nav,
  .user-rail,
  .controls-wrap {
    width: 100%;
  }

  .brand {
    gap: 12px;
  }

  .mark {
    width: 48px;
    height: 48px;
    border-radius: 16px;
  }

  .brand strong {
    font-size: 1.12rem;
  }

  .topbar-subtitle {
    font-size: 0.8rem;
  }

  .editorial-nav {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding: 4px;
    gap: 6px;
    scrollbar-width: none;
  }

  .editorial-nav::-webkit-scrollbar {
    display: none;
  }

  .nav a {
    width: auto;
    min-width: 88px;
    padding: 0 14px;
    text-align: center;
    flex: 0 0 auto;
  }

  .auth-btn {
    width: 100%;
    text-align: center;
  }

  .controls-wrap {
    justify-content: flex-start;
  }

  .controls-wrap :deep(.app-controls) {
    width: 100%;
  }
}
</style>
