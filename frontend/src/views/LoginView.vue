<script setup>
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { loginApi, registerApi } from '../api/auth';
import AppControls from '../components/AppControls.vue';
import { useAuthStore } from '../stores/auth';
import { usePreferencesStore } from '../stores/preferences';

const preferences = usePreferencesStore();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const activeTab = ref('login');

const loginForm = reactive({
  username: '',
  password: ''
});

const registerForm = reactive({
  username: '',
  email: '',
  displayName: '',
  password: ''
});

const copy = computed(() =>
  preferences.locale === 'zh-CN'
    ? {
        title: '账号登录',
        subtitle: '登录后才能发表评论、发起 RAG 对话，并配置你的千问 Key。',
        login: '登录',
        register: '注册',
        username: '用户名',
        email: '邮箱',
        displayName: '显示名称',
        password: '密码',
        signIn: '立即登录',
        signUp: '注册并登录',
        backToSite: '返回网站',
        loginSuccess: '登录成功',
        registerSuccess: '注册成功',
        passwordHint: '密码至少 8 位'
      }
    : {
        title: 'Account Access',
        subtitle: 'Sign in to comment, start RAG chats, and configure your Qwen API key.',
        login: 'Sign In',
        register: 'Register',
        username: 'Username',
        email: 'Email',
        displayName: 'Display Name',
        password: 'Password',
        signIn: 'Sign In',
        signUp: 'Create Account',
        backToSite: 'Back to Site',
        loginSuccess: 'Login successful',
        registerSuccess: 'Register successful',
        passwordHint: 'Password must be at least 8 characters'
      }
);

function resolveRedirect() {
  if (typeof route.query.redirect === 'string' && route.query.redirect) {
    return route.query.redirect;
  }
  return authStore.isAdmin ? '/admin/dashboard' : '/';
}

async function login() {
  const res = await loginApi(loginForm);
  authStore.setSession(res.data);
  ElMessage.success(copy.value.loginSuccess);
  router.push(resolveRedirect());
}

async function register() {
  const res = await registerApi(registerForm);
  authStore.setSession(res.data);
  ElMessage.success(copy.value.registerSuccess);
  router.push(resolveRedirect());
}
</script>

<template>
  <div class="login-shell">
    <div class="login-toolbar">
      <AppControls />
    </div>

    <div class="login-card section-card">
      <div>
        <span class="hero-kicker">Account</span>
        <h1>{{ copy.title }}</h1>
        <p class="muted login-copy">{{ copy.subtitle }}</p>
      </div>

      <div class="tab-row">
        <button
          type="button"
          class="tab-btn"
          :class="{ active: activeTab === 'login' }"
          @click="activeTab = 'login'"
        >
          {{ copy.login }}
        </button>
        <button
          type="button"
          class="tab-btn"
          :class="{ active: activeTab === 'register' }"
          @click="activeTab = 'register'"
        >
          {{ copy.register }}
        </button>
      </div>

      <el-form v-if="activeTab === 'login'" label-position="top" @submit.prevent="login">
        <el-form-item :label="copy.username">
          <el-input v-model="loginForm.username" />
        </el-form-item>
        <el-form-item :label="copy.password">
          <el-input v-model="loginForm.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" class="full" @click="login">{{ copy.signIn }}</el-button>
        <el-button class="full plain" @click="$router.push('/')">{{ copy.backToSite }}</el-button>
      </el-form>

      <el-form v-else label-position="top" @submit.prevent="register">
        <el-form-item :label="copy.username">
          <el-input v-model="registerForm.username" />
        </el-form-item>
        <el-form-item :label="copy.email">
          <el-input v-model="registerForm.email" />
        </el-form-item>
        <el-form-item :label="copy.displayName">
          <el-input v-model="registerForm.displayName" />
        </el-form-item>
        <el-form-item :label="copy.password">
          <el-input v-model="registerForm.password" type="password" show-password />
          <p class="muted password-hint">{{ copy.passwordHint }}</p>
        </el-form-item>
        <el-button type="primary" class="full" @click="register">{{ copy.signUp }}</el-button>
        <el-button class="full plain" @click="$router.push('/')">{{ copy.backToSite }}</el-button>
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
    radial-gradient(circle at top, rgba(220, 193, 136, 0.18), transparent 24%),
    linear-gradient(180deg, #17130f 0%, #100d0a 100%);
}

html[data-theme='light'] .login-shell {
  background:
    radial-gradient(circle at top, rgba(181, 143, 76, 0.12), transparent 24%),
    linear-gradient(180deg, #faf5ec 0%, #efe7da 100%);
}

.login-toolbar {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 10;
}

.login-card {
  width: min(560px, 100%);
  padding: 40px;
}

.login-copy {
  margin: 0 0 6px;
  line-height: 1.8;
}

h1 {
  margin: 0 0 10px;
  font-size: clamp(2.2rem, 6vw, 2.8rem);
  letter-spacing: -0.05em;
}

.tab-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 22px 0 18px;
}

.tab-btn {
  min-height: 44px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font: inherit;
}

.tab-btn.active {
  color: var(--text-main);
  background: var(--bg-panel-strong);
  border-color: var(--line-strong);
}

.full {
  width: 100%;
  margin-top: 8px;
}

.plain {
  margin-left: 0;
}

.password-hint {
  margin: 8px 0 0;
  font-size: 0.82rem;
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
