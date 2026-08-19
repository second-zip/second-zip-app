<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { getApiError } from '@/api/utils/error';
import MyPageHeader from '@/components/mypage/MyPageHeader.vue';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const router = useRouter();
const password = ref('');
const agreed = ref(false);
const errorMessage = ref('');
const submitting = ref(false);

const handleWithdraw = async () => {
  errorMessage.value = '';
  if (!password.value || !agreed.value) { errorMessage.value = '비밀번호 입력과 탈퇴 동의가 필요합니다.'; return; }
  submitting.value = true;
  try { await authStore.withdraw(password.value); await router.replace({ name: 'main' }); }
  catch (error) { errorMessage.value = getApiError(error).message; }
  finally { submitting.value = false; }
};
</script>

<template>
  <div class="withdraw-page"><MyPageHeader title="회원 탈퇴" back />
    <main class="withdraw-page__body"><section class="withdraw-card">
      <div class="withdraw-card__icon" aria-hidden="true">!</div><h2>정말 탈퇴하시겠어요?</h2>
      <p>탈퇴하면 계정 정보와 저장된 리포트를 다시 복구할 수 없습니다.</p>
      <form @submit.prevent="handleWithdraw"><label>현재 비밀번호<input v-model="password" type="password" autocomplete="current-password" placeholder="비밀번호를 입력해 주세요" /></label>
        <label class="withdraw-card__agree"><input v-model="agreed" type="checkbox" /> 안내 내용을 확인했으며 회원 탈퇴에 동의합니다.</label>
        <p v-if="errorMessage" class="withdraw-card__error" role="alert">{{ errorMessage }}</p>
        <button type="submit" :disabled="submitting">{{ submitting ? '탈퇴 처리 중...' : '회원 탈퇴' }}</button>
      </form></section>
    </main>
  </div>
</template>

<style scoped>
.withdraw-page { min-height: 100%; background: #f7f9fd; }.withdraw-page__body { padding: 24px 20px; }.withdraw-card { padding: 28px 20px; border: 1px solid #e1e6ee; border-radius: 18px; background: #fff; text-align: center; }.withdraw-card__icon { width: 46px; height: 46px; margin: 0 auto 14px; color: #ee3f46; border-radius: 50%; background: #ffe7e8; font-size: 28px; font-weight: 800; }.withdraw-card h2 { font-size: 18px; }.withdraw-card > p { margin-bottom: 26px; color: #6c7687; font-size: 13px; line-height: 1.6; }.withdraw-card form { text-align: left; }.withdraw-card form > label:first-child { display: grid; gap: 8px; font-size: 12px; font-weight: 600; }.withdraw-card input[type='password'] { height: 46px; padding: 0 13px; border: 1px solid #d6dce6; border-radius: 10px; }.withdraw-card__agree { display: flex; gap: 8px; align-items: flex-start; margin: 18px 0; font-size: 12px; }.withdraw-card__error { color: #d9363e; font-size: 12px; }.withdraw-card button { width: 100%; height: 46px; border: 0; border-radius: 11px; color: #fff; background: #e54249; font-weight: 700; }.withdraw-card button:disabled { opacity: .55; }

@media (min-width: 768px) {
  .withdraw-page__body {
    width: min(100%, 640px);
    margin: 0 auto;
    padding: 40px 32px;
  }
}
</style>
