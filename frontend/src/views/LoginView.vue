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
        title: '\u8d26\u53f7\u767b\u5f55',
        subtitle: '\u767b\u5f55\u540e\u624d\u80fd\u53d1\u8868\u8bc4\u8bba\u3001\u53d1\u8d77 RAG \u5bf9\u8bdd\uff0c\u5e76\u914d\u7f6e\u4f60\u7684\u5343\u95ee Key\u3002',
        login: '\u767b\u5f55',
        register: '\u6ce8\u518c',
        username: '\u7528\u6237\u540d',
        email: '\u90ae\u7bb1',
        displayName: '\u663e\u793a\u540d\u79f0',
        password: '\u5bc6\u7801',
        signIn: '\u7acb\u5373\u767b\u5f55',
        signUp: '\u6ce8\u518c\u5e76\u767b\u5f55',
        backToSite: '\u8fd4\u56de\u7f51\u7ad9',
        loginSuccess: '\u767b\u5f55\u6210\u529f',
        registerSuccess: '\u6ce8\u518c\u6210\u529f',
        passwordHint: '\u5bc6\u7801\u81f3\u5c11 8 \u4f4d',
        loginTip: '\u82e5\u767b\u5f55\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u7528\u6237\u540d\u662f\u5426\u8f93\u5165\u6b63\u786e\uff0c\u6216\u786e\u8ba4\u5bc6\u7801\u662f\u5426\u533a\u5206\u5927\u5c0f\u5199\u3002',
        registerTip: '\u6ce8\u518c\u65f6\u8bf7\u586b\u5199\u672a\u88ab\u5360\u7528\u7684\u7528\u6237\u540d\u548c\u90ae\u7bb1\uff0c\u6ce8\u518c\u6210\u529f\u540e\u5c06\u81ea\u52a8\u767b\u5f55\u3002'
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
        passwordHint: 'Password must be at least 8 characters',
        loginTip: 'If sign-in fails, check whether the username is correct and whether the password uses the expected letter case.',
        registerTip: 'Use a unique username and email when registering. You will be signed in automatically after success.'
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
        <p v-if="activeTab === 'login'" class="muted form-tip">{{ copy.loginTip }}</p>
        <p v-else class="muted form-tip">{{ copy.registerTip }}</p>
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

.form-tip {
  margin: 0;
  line-height: 1.7;
  font-size: 0.92rem;
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
