<script setup>
import { reactive, ref } from 'vue';

import { getApiError } from '@/api/utils/error';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();

// API 요청 Body와 동일한 구조로 폼 상태를 관리한다.
const form = reactive({
  email: '',
  password: '',
  passwordConfirm: '',
  nickname: '',
  termsAgreed: false,
  privacyAgreed: false,
});

const errorMessage = ref('');
const successMessage = ref('');

const validateForm = () => {
  // 서버 요청 전에 필수값과 명세로 확정된 기본 규칙을 우선 검사한다.
  // TODO: 백엔드와 비밀번호 및 닉네임 정책을 확정한 뒤 상세 검증을 추가한다.
  if (!form.email || !form.password || !form.passwordConfirm || !form.nickname) {
    return '필수 항목을 모두 입력해주세요.';
  }

  if (!/^\S+@\S+\.\S+$/.test(form.email)) {
    return '올바른 이메일 형식을 입력해주세요.';
  }

  if (form.password !== form.passwordConfirm) {
    return '비밀번호와 비밀번호 확인이 일치하지 않습니다.';
  }

  // TODO: 약관 버전 정보가 확정되면 동의 항목과 함께 전달할지 확인한다.
  if (!form.termsAgreed || !form.privacyAgreed) {
    return '이용약관과 개인정보처리방침에 동의해주세요.';
  }

  return '';
};

const handleSignup = async () => {
  errorMessage.value = validateForm();
  successMessage.value = '';

  if (errorMessage.value) {
    return;
  }

  // 검증을 통과한 폼을 전송하고 성공한 사용자 정보를 화면에 반영한다.
  try {
    const user = await authStore.signup({ ...form });

    successMessage.value = `${user.nickname}님의 회원가입이 완료되었습니다.`;
  } catch (error) {
    // 백엔드 오류 코드는 공통 유틸에서 해석하고 사용자용 메시지를 표시한다.
    errorMessage.value = getApiError(error).message;
  }
};
</script>

<template>
  <main>
    <h1>회원가입</h1>

    <form @submit.prevent="handleSignup">
      <label>
        이메일
        <input v-model.trim="form.email" type="email" required />
      </label>

      <label>
        비밀번호
        <input v-model="form.password" type="password" required />
      </label>

      <label>
        비밀번호 확인
        <input v-model="form.passwordConfirm" type="password" required />
      </label>

      <label>
        닉네임
        <input v-model.trim="form.nickname" type="text" required />
      </label>

      <label>
        <input v-model="form.termsAgreed" type="checkbox" required />
        이용약관에 동의합니다.
      </label>

      <label>
        <input v-model="form.privacyAgreed" type="checkbox" required />
        개인정보처리방침에 동의합니다.
      </label>

      <p v-if="errorMessage">{{ errorMessage }}</p>
      <p v-if="successMessage">{{ successMessage }}</p>

      <button type="submit" :disabled="authStore.loading">
        {{ authStore.loading ? '처리 중...' : '회원가입' }}
      </button>
    </form>
  </main>
</template>
