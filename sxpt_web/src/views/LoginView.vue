<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authApi } from '../services/trainingApi';
import { useTrainingStore } from '../stores/trainingStore';

const route = useRoute();
const router = useRouter();
const store = useTrainingStore();

const form = reactive({
  tenantId: 'demo-tenant',
  username: '',
  password: ''
});
const loading = ref(false);
const errorMessage = ref('');

const canSubmit = computed(
  () =>
    form.tenantId.trim().length > 0 &&
    form.username.trim().length > 0 &&
    form.password.length > 0 &&
    !loading.value
);

/**
 * 执行真实登录。
 *
 * 业务功能：
 * 1. 使用后端认证接口校验账号密码，前端不自行构造用户身份。
 * 2. 登录成功后根据服务端返回的用户角色进入对应门户。
 *
 * 关键流程：
 * 1. 清理表单空白并提交 PASSWORD 登录请求。
 * 2. 保存后端签发的 token 和用户摘要。
 * 3. 优先回到被拦截的原始地址，否则进入角色默认首页。
 */
async function submitLogin() {
  if (!canSubmit.value) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    await authApi.login({
      loginType: 'PASSWORD',
      tenantId: form.tenantId.trim(),
      username: form.username.trim(),
      password: form.password
    });
    await store.initializeAuthenticatedWorkspace(session);
    const redirect = typeof route.query.redirect === 'string'
      ? route.query.redirect
      : '/platforms';
    await router.replace(redirect);
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '登录失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel" aria-labelledby="login-title">
      <div class="login-brand">
        <span>SX</span>
        <div>
          <strong>实训平台</strong>
          <small>教师 / 学生 / 后台统一入口</small>
        </div>
      </div>

      <form class="login-form" @submit.prevent="submitLogin">
        <header>
          <h1 id="login-title">账号登录</h1>
          <p>使用教学平台真实账号进入对应工作台。</p>
        </header>

        <label>
          租户
          <input v-model="form.tenantId" autocomplete="organization" />
        </label>

        <label>
          用户名
          <input v-model="form.username" autocomplete="username" autofocus />
        </label>

        <label>
          密码
          <input
            v-model="form.password"
            autocomplete="current-password"
            type="password"
          />
        </label>

        <p v-if="errorMessage" class="login-error">{{ errorMessage }}</p>

        <button class="primary" type="submit" :disabled="!canSubmit">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 28px;
  background:
    linear-gradient(135deg, rgb(46 117 240 / 12%), transparent 36%),
    linear-gradient(315deg, rgb(18 168 121 / 10%), transparent 34%),
    #f4f6fb;
}

.login-panel {
  display: grid;
  width: min(920px, 100%);
  grid-template-columns: minmax(260px, 0.85fr) minmax(320px, 1fr);
  overflow: hidden;
  border: 1px solid #e2e7f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 22px 70px rgb(23 32 51 / 12%);
}

.login-brand {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  min-height: 520px;
  padding: 38px;
  background:
    linear-gradient(160deg, rgb(23 32 51 / 82%), rgb(23 32 51 / 96%)),
    linear-gradient(135deg, #2e75f0, #12a879);
  color: #fff;
}

.login-brand > span {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 8px;
  background: rgb(255 255 255 / 14%);
  font-weight: 900;
}

.login-brand strong {
  display: block;
  font-size: 26px;
}

.login-brand small {
  color: rgb(255 255 255 / 72%);
  font-size: 13px;
}

.login-form {
  display: grid;
  align-content: center;
  gap: 18px;
  padding: 52px;
}

.login-form header {
  display: grid;
  gap: 8px;
  margin-bottom: 8px;
}

.login-form h1 {
  margin: 0;
  color: #172033;
  font-size: 30px;
}

.login-form p {
  margin: 0;
  color: #6b778c;
}

.login-error {
  border: 1px solid #ffd2d6;
  border-radius: 8px;
  background: #fff5f5;
  color: #c53b49 !important;
  padding: 10px 12px;
  font-size: 13px;
}

.login-form button {
  margin-top: 4px;
}

@media (max-width: 760px) {
  .login-page {
    padding: 14px;
  }

  .login-panel {
    grid-template-columns: 1fr;
  }

  .login-brand {
    min-height: 160px;
    padding: 26px;
  }

  .login-form {
    padding: 28px;
  }
}
</style>
