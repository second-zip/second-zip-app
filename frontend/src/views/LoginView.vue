<script setup>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getApiErrorMessage } from '@/api/utils/error';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const form = reactive({
  email: '',
  password: '',
});
const errorMessage = ref('');

// 세진: 입력한 계정으로 로그인한 뒤 이전 페이지 또는 마이페이지로 이동한다.
const handleLogin = async () => {
  errorMessage.value = '';

  try {
    await authStore.login(form);

    const redirectPath =
      typeof route.query.redirect === 'string' ? route.query.redirect : '/mypage';

    await router.replace(redirectPath);
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error);
  }
};
</script>

<template>
  <main class="p-4">
    <h1 class="h3 mb-4">로그인</h1>

    <form @submit.prevent="handleLogin">
      <label class="form-label w-100 mb-3">
        이메일
        <input v-model.trim="form.email" class="form-control mt-1" type="email" required />
      </label>

      <label class="form-label w-100 mb-3">
        비밀번호
        <input v-model="form.password" class="form-control mt-1" type="password" required />
      </label>

      <p v-if="errorMessage" class="text-danger">{{ errorMessage }}</p>

      <button class="btn btn-primary w-100" type="submit" :disabled="authStore.loading">
        {{ authStore.loading ? '로그인 중...' : '로그인' }}
      </button>
    </form>
  </main>
</template>
