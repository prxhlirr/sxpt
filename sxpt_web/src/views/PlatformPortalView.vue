<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { PortalRole } from '../domain/models';
import { useTrainingStore } from '../stores/trainingStore';

const route = useRoute();
const router = useRouter();
const store = useTrainingStore();
const feedback = ref(
  route.query.access === 'denied'
    ? '当前登录身份无权访问该功能，请选择可用平台。'
    : ''
);

const identityProfiles: Record<
  PortalRole,
  { label: string; name: string; destination: string; destinationLabel: string }
> = {
  admin: {
    label: '平台管理员',
    name: '薛管理员',
    destination: '/admin/overview',
    destinationLabel: '后台管理'
  },
  teacher: {
    label: '授课教师',
    name: '薛老师',
    destination: '/teacher/dashboard',
    destinationLabel: '教师端'
  },
  student: {
    label: '参训学员',
    name: '林同学',
    destination: '/student/tasks',
    destinationLabel: '学生端'
  }
};

const identity = computed(() => identityProfiles[store.state.currentRole]);
const showDevelopmentEntrances = import.meta.env.DEV;
const premiumClassroomUrl = String(
  import.meta.env.VITE_PREMIUM_CLASSROOM_URL ?? ''
).trim();

function openPremiumClassroom() {
  if (!premiumClassroomUrl) {
    feedback.value = '精品课堂跳转地址尚未配置，请设置 VITE_PREMIUM_CLASSROOM_URL。';
    return;
  }
  window.location.assign(premiumClassroomUrl);
}

async function openTrainingPlatform() {
  await router.push(identity.value.destination);
}

async function enterDevelopmentPortal(role: PortalRole) {
  store.setRole(role);
  await router.push(identityProfiles[role].destination);
}
</script>

<template>
  <main class="platform-portal">
    <header class="portal-header">
      <div class="portal-brand">
        <span>SX</span>
        <div>
          <strong>智慧教学与实训中心</strong>
          <small>SMART LEARNING CENTER</small>
        </div>
      </div>
      <div class="portal-identity">
        <span>{{ identity.name.slice(0, 1) }}</span>
        <div>
          <small>当前登录身份</small>
          <strong>{{ identity.name }} · {{ identity.label }}</strong>
        </div>
      </div>
    </header>

    <section class="portal-hero">
      <span class="portal-eyebrow">WELCOME BACK</span>
      <h1>请选择要进入的平台</h1>
      <p>系统已根据登录账号识别身份，进入实训平台后将自动打开对应工作端。</p>
    </section>

    <section class="platform-grid" aria-label="平台选择">
      <button class="platform-card classroom-card" type="button" @click="openPremiumClassroom">
        <span class="platform-card__glow" />
        <span class="platform-icon">课</span>
        <span class="platform-card__content">
          <small>PREMIUM CLASSROOM</small>
          <strong>精品课堂</strong>
          <em>优质课程资源、在线学习与课堂互动</em>
        </span>
        <span class="platform-card__footer">
          <span>外部平台</span>
          <b>进入平台 ↗</b>
        </span>
      </button>

      <button class="platform-card training-card" type="button" @click="openTrainingPlatform">
        <span class="platform-card__glow" />
        <span class="platform-icon">训</span>
        <span class="platform-card__content">
          <small>BUSINESS TRAINING</small>
          <strong>实训平台</strong>
          <em>业务教案录制、考试组织与实训任务办理</em>
        </span>
        <span class="platform-card__footer">
          <span>将进入{{ identity.destinationLabel }}</span>
          <b>进入平台 →</b>
        </span>
      </button>
    </section>

    <div v-if="feedback" class="portal-feedback" role="status">
      <span>i</span>
      {{ feedback }}
      <button type="button" aria-label="关闭提示" @click="feedback = ''">×</button>
    </div>

    <aside v-if="showDevelopmentEntrances" class="development-entry">
      <span>DEV</span>
      <div>
        <strong>开发环境快捷入口</strong>
        <small>切换模拟登录身份，不会出现在生产环境</small>
      </div>
      <div class="development-entry__actions">
        <button type="button" @click="enterDevelopmentPortal('admin')">管理端</button>
        <button type="button" @click="enterDevelopmentPortal('teacher')">教师端</button>
        <button type="button" @click="enterDevelopmentPortal('student')">学生端</button>
      </div>
    </aside>

    <footer class="portal-footer">
      <span>业务实训平台 · 统一应用门户</span>
      <span>身份权限已启用</span>
    </footer>
  </main>
</template>

<style scoped>
.platform-portal {
  position: relative;
  display: flex;
  min-height: 100vh;
  overflow: hidden;
  flex-direction: column;
  padding: 0 clamp(24px, 6vw, 96px);
  color: #f7f8ff;
  background:
    radial-gradient(circle at 12% 12%, rgb(119 91 255 / 25%), transparent 32%),
    radial-gradient(circle at 88% 78%, rgb(20 168 145 / 18%), transparent 32%),
    linear-gradient(135deg, #111528 0%, #171b34 52%, #101827 100%);
}

.platform-portal::before,
.platform-portal::after {
  position: absolute;
  border: 1px solid rgb(255 255 255 / 5%);
  border-radius: 50%;
  content: "";
  pointer-events: none;
}

.platform-portal::before {
  top: -280px;
  right: -180px;
  width: 700px;
  height: 700px;
}

.platform-portal::after {
  bottom: -350px;
  left: -240px;
  width: 820px;
  height: 820px;
}

.portal-header {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 88px;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  border-bottom: 1px solid rgb(255 255 255 / 8%);
}

.portal-brand,
.portal-identity {
  display: flex;
  align-items: center;
  gap: 12px;
}

.portal-brand > span {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 13px;
  color: #fff;
  background: linear-gradient(145deg, #8b7bff, #5f4de7);
  box-shadow: 0 12px 28px rgb(92 72 226 / 35%);
  font-size: 14px;
  font-weight: 900;
}

.portal-brand > div,
.portal-identity > div {
  display: grid;
  gap: 3px;
}

.portal-brand strong {
  font-size: 16px;
}

.portal-brand small,
.portal-identity small {
  color: #888fa9;
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.portal-identity {
  border: 1px solid rgb(255 255 255 / 9%);
  border-radius: 999px;
  padding: 7px 15px 7px 8px;
  background: rgb(255 255 255 / 4%);
}

.portal-identity > span {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  color: #dcd8ff;
  background: rgb(121 101 241 / 22%);
  font-size: 11px;
  font-weight: 900;
}

.portal-identity strong {
  font-size: 10px;
}

.portal-hero {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  margin: auto 0 38px;
  text-align: center;
}

.portal-eyebrow {
  margin-bottom: 13px;
  color: #9184f9;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.22em;
}

.portal-hero h1 {
  margin: 0;
  font-size: clamp(30px, 4vw, 48px);
  letter-spacing: -0.04em;
}

.portal-hero p {
  max-width: 640px;
  margin: 13px 0 0;
  color: #9ba3b9;
  font-size: 13px;
  line-height: 1.8;
}

.platform-grid {
  position: relative;
  z-index: 1;
  display: grid;
  width: min(1040px, 100%);
  grid-template-columns: 1fr 1fr;
  gap: 22px;
  margin: 0 auto auto;
}

.platform-card {
  position: relative;
  display: grid;
  min-height: 300px;
  overflow: hidden;
  grid-template-columns: 70px minmax(0, 1fr);
  align-content: start;
  gap: 20px;
  border: 1px solid rgb(255 255 255 / 10%);
  border-radius: 22px;
  padding: 30px;
  color: #fff;
  background: rgb(255 255 255 / 6%);
  box-shadow: 0 28px 70px rgb(0 0 0 / 20%);
  text-align: left;
  backdrop-filter: blur(18px);
}

.platform-card:hover {
  border-color: rgb(155 141 255 / 52%);
  background: rgb(255 255 255 / 9%);
  box-shadow: 0 34px 80px rgb(0 0 0 / 30%);
  transform: translateY(-5px);
}

.training-card:hover {
  border-color: rgb(70 202 181 / 52%);
}

.platform-card__glow {
  position: absolute;
  top: -100px;
  right: -80px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: rgb(118 91 255 / 18%);
  filter: blur(4px);
}

.training-card .platform-card__glow {
  background: rgb(19 169 144 / 16%);
}

.platform-icon {
  display: grid;
  width: 66px;
  height: 66px;
  place-items: center;
  border: 1px solid rgb(255 255 255 / 15%);
  border-radius: 18px;
  color: #e7e3ff;
  background: linear-gradient(145deg, rgb(130 109 255 / 36%), rgb(87 70 204 / 15%));
  font-size: 22px;
  font-weight: 900;
}

.training-card .platform-icon {
  color: #d7fff8;
  background: linear-gradient(145deg, rgb(29 179 154 / 34%), rgb(12 105 91 / 15%));
}

.platform-card__content {
  position: relative;
  display: grid;
  align-content: start;
  gap: 8px;
  padding-top: 4px;
}

.platform-card__content small {
  color: #8f83f4;
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.16em;
}

.training-card .platform-card__content small {
  color: #45bfa9;
}

.platform-card__content strong {
  font-size: 28px;
}

.platform-card__content em {
  color: #a6aec0;
  font-style: normal;
  font-size: 11px;
  line-height: 1.7;
}

.platform-card__footer {
  position: absolute;
  right: 30px;
  bottom: 27px;
  left: 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid rgb(255 255 255 / 9%);
  padding-top: 20px;
  color: #8992a8;
  font-size: 10px;
}

.platform-card__footer b {
  color: #fff;
  font-size: 11px;
}

.portal-feedback {
  position: relative;
  z-index: 2;
  display: flex;
  width: min(720px, 100%);
  align-items: center;
  gap: 9px;
  margin: 22px auto 0;
  border: 1px solid rgb(150 137 246 / 25%);
  border-radius: 10px;
  padding: 10px 12px;
  color: #c0c5d3;
  background: rgb(104 84 221 / 10%);
  font-size: 10px;
}

.portal-feedback > span {
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #6d5dfc;
  font-weight: 900;
}

.portal-feedback button {
  min-height: 24px;
  margin-left: auto;
  border: 0;
  padding: 0 5px;
  color: #aeb5c7;
  background: transparent;
}

.development-entry {
  position: fixed;
  z-index: 5;
  right: 24px;
  bottom: 24px;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  min-width: 470px;
  align-items: center;
  gap: 10px;
  border: 1px solid rgb(255 191 95 / 28%);
  border-radius: 12px;
  padding: 10px;
  color: #f4f5fb;
  background: rgb(35 37 56 / 92%);
  box-shadow: 0 16px 42px rgb(0 0 0 / 28%);
  backdrop-filter: blur(14px);
}

.development-entry > span {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  color: #231d14;
  background: #ffbc5b;
  font-size: 8px;
  font-weight: 900;
}

.development-entry > div {
  display: grid;
  gap: 3px;
}

.development-entry strong {
  font-size: 10px;
}

.development-entry small {
  color: #939bae;
  font-size: 8px;
}

.development-entry > .development-entry__actions {
  display: flex;
  gap: 5px;
}

.development-entry button {
  min-height: 31px;
  border-color: rgb(255 255 255 / 13%);
  border-radius: 7px;
  padding: 0 10px;
  color: #fff;
  background: rgb(255 255 255 / 8%);
  font-size: 9px;
}

.development-entry button:hover {
  border-color: rgb(255 188 91 / 45%);
  background: rgb(255 188 91 / 13%);
}

.portal-footer {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  color: #687087;
  font-size: 9px;
}

@media (max-width: 760px) {
  .platform-portal {
    padding-inline: 18px;
  }

  .portal-header {
    min-height: 76px;
  }

  .portal-brand strong {
    font-size: 13px;
  }

  .portal-brand small,
  .portal-identity > div {
    display: none;
  }

  .portal-hero {
    margin-top: 55px;
  }

  .platform-grid {
    grid-template-columns: 1fr;
  }

  .platform-card {
    min-height: 240px;
  }

  .development-entry {
    right: 14px;
    bottom: 14px;
    left: 14px;
    min-width: 0;
  }
}

@media (max-width: 440px) {
  .platform-card {
    grid-template-columns: 52px 1fr;
    min-height: 225px;
    gap: 14px;
    padding: 22px;
  }

  .platform-icon {
    width: 50px;
    height: 50px;
    border-radius: 14px;
    font-size: 17px;
  }

  .platform-card__content strong {
    font-size: 22px;
  }

  .platform-card__footer {
    right: 22px;
    bottom: 21px;
    left: 22px;
  }

  .development-entry {
    grid-template-columns: 30px 1fr;
  }

  .development-entry > span {
    width: 30px;
    height: 30px;
  }

  .development-entry > .development-entry__actions {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
