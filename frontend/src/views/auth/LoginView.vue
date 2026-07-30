<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getApiErrorMessage } from '@/api/utils/error';
import { useAuthStore } from '@/stores/auth';

import AuthInputBox from '@/components/auth/AuthInputBox.vue';
import BaseButton from '@/components/common/BaseButton.vue';

import LoginImage from '@/assets/images/second-zip.png';

const router = useRouter();
const authStore = useAuthStore();

const form = reactive({
  email: '',
  password: '',
});

const errorMessage = ref('');

const handleLogin = async () => {
  errorMessage.value = '';

  try {
    await authStore.login(form);

    await router.replace('/main');
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error);
  }
};
</script>

<template>
  <main class="login-page position-relative w-100">
    <div
      class="login-page__visual position-absolute w-100 d-flex align-items-center justify-content-center"
    >
      <img :src="LoginImage" alt="이번집 서비스 소개" />
    </div>

    <form
      class="login-page__form position-absolute start-50 w-100 d-flex flex-column gap-4"
      @submit.prevent="handleLogin"
    >
      <AuthInputBox
        id="email"
        v-model="form.email"
        type="email"
        label="이메일"
        autocomplete="email"
      />
      <AuthInputBox
        id="password"
        v-model="form.password"
        type="password"
        label="비밀번호"
      />
      <p v-if="errorMessage" class="error-message fs-6 mb-0 fw-semibold w-100">
        {{ errorMessage }}
      </p>
      <BaseButton type="submit" :disabled="authStore.loading">{{
        authStore.loading ? '로그인 중...' : '로그인'
      }}</BaseButton>
    </form>
  </main>
</template>

<style scoped>
.login-page {
  height: 100%;
  min-height: 100%;
  overflow: hidden;
  background-color: #fff;
}

.login-page__visual {
  top: 0;
  left: 0;
  height: calc(50% - 64px);
}

.login-page__form {
  top: calc(50% + 40px);
  max-width: 402px;
  padding: 0 28px;
  transform: translate(-50%, -50%);
}

.error-message {
  color: var(--red-500);
  text-align: center;
}
</style>
