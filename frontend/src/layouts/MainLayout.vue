<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import AppControls from '../components/AppControls.vue';
import { usePreferencesStore } from '../stores/preferences';

const preferences = usePreferencesStore();
const route = useRoute();
const immersiveMode = computed(() => Boolean(route.meta.immersive));
const hideFooter = computed(() => Boolean(route.meta.hideFooter));

const navItems = computed(() => [
  { label: preferences.t('main.navHome'), to: '/' },
  { label: preferences.locale === 'zh-CN' ? '知识问答' : 'Knowledge', to: '/knowledge' },
  { label: preferences.t('main.navArchive'), to: '/archives' },
  { label: preferences.t('main.navCategories'), to: '/categories' },
  { label: preferences.t('main.navConsole'), to: '/admin' }
]);
</script>

<template>
  <div class="page-shell" :class="{ 'page-shell-immersive': immersiveMode }">
    <header class="topbar glass-panel" :class="{ 'topbar-immersive': immersiveMode }">
      <router-link to="/" class="brand">
        <span class="mark">HJ</span>
        <div>
          <strong>HeJulian Blog</strong>
          <p>{{ preferences.t('main.brandSubtitle') }}</p>
        </div>
      </router-link>

      <div class="topbar-actions">
        <nav class="nav">
          <router-link v-for="item in navItems" :key="item.to" :to="item.to">
            {{ item.label }}
          </router-link>
        </nav>
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
  padding: 12px 12px 24px;
}

.topbar {
  position: relative;
  z-index: 12;
  margin-bottom: 30px;
  padding: 18px 22px;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.topbar-immersive {
  position: sticky;
  top: 12px;
  z-index: 20;
  margin-bottom: 12px;
  border-radius: 22px;
  backdrop-filter: blur(24px);
}

.brand {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand strong {
  display: block;
  font-size: 1.05rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.brand p,
.footer {
  margin: 0;
}

.brand p {
  color: var(--text-secondary);
  font-size: 0.88rem;
}

.mark {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  font-weight: 800;
  letter-spacing: 0.18em;
  color: #050505;
  background: linear-gradient(135deg, #ffffff, #a8a8a8);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8), 0 10px 24px rgba(255, 255, 255, 0.12);
}

.topbar-actions {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  flex-wrap: wrap;
}

.nav {
  position: relative;
  z-index: 3;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.nav a {
  position: relative;
  z-index: 3;
  padding: 10px 14px;
  border-radius: 999px;
  border: 1px solid transparent;
  color: var(--text-secondary);
  transition: 0.2s ease;
}

.nav a:hover,
.nav .router-link-active {
  color: var(--text-main);
  background: var(--bg-panel);
  border-color: var(--line);
}

.footer {
  padding: 30px 4px 0;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  font-size: 0.84rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.main-immersive {
  min-height: calc(100vh - 110px);
}

@media (max-width: 960px) {
  .topbar {
    align-items: flex-start;
  }

  .topbar-actions {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 720px) {
  .page-shell-immersive {
    padding: 8px;
  }

  .topbar-immersive {
    top: 8px;
    margin-bottom: 8px;
  }

  .topbar,
  .footer,
  .topbar-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
