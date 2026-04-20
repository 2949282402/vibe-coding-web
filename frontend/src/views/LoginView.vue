<script setup>
import { reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { loginApi } from '../api/auth';
import AppControls from '../components/AppControls.vue';
import { useAuthStore } from '../stores/auth';
import { usePreferencesStore } from '../stores/preferences';

const preferences = usePreferencesStore();
const form = reactive({
  username: 'admin',
  password: 'Admin123!'
});

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const login = async () => {
  const res = await loginApi(form);
  authStore.setSession(res.data);
  ElMessage.success(preferences.t('login.loginSuccess'));
  router.push(route.query.redirect || '/admin/dashboard');
};
</script>

<template>
  <div class="login-shell">
    <div class="login-toolbar">
      <AppControls />
    </div>

    <div class="login-card section-card">
      <div>
        <span class="hero-kicker">{{ preferences.t('login.adminAccess') }}</span>
        <h1>{{ preferences.t('login.enterConsole') }}</h1>
        <p class="muted login-copy">{{ preferences.t('login.defaultAccount') }}</p>
      </div>

      <el-form label-position="top" @submit.prevent="login">
        <el-form-item :label="preferences.t('login.username')">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item :label="preferences.t('login.password')">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" class="full" @click="login">{{ preferences.t('login.signIn') }}</el-button>
        <el-button class="full plain" @click="$router.push('/')">{{ preferences.t('login.backToSite') }}</el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.08), transparent 22%),
    linear-gradient(180deg, #080808 0%, #030303 100%);
}

html[data-theme='light'] .login-shell {
  background:
    radial-gradient(circle at top, rgba(0, 0, 0, 0.05), transparent 24%),
    linear-gradient(180deg, #f7f7f4 0%, #ecece8 100%);
}

.login-toolbar {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 10;
}

.login-card {
  width: min(500px, 100%);
  padding: 36px;
}

.login-copy {
  margin: 0 0 6px;
  line-height: 1.8;
}

h1 {
  margin: 0 0 10px;
  font-size: clamp(2.2rem, 6vw, 2.8rem);
  letter-spacing: -0.05em;
  text-transform: none;
}

.full {
  width: 100%;
  margin-top: 8px;
}

.plain {
  margin-left: 0;
}

@media (max-width: 720px) {
  .login-toolbar {
    position: static;
    justify-self: start;
    margin-bottom: 18px;
  }

  .login-shell {
    place-items: start stretch;
  }

  .login-card {
    padding: 26px;
  }
}
</style>
