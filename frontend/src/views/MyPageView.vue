<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getApiError } from '@/api/utils/error';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const router = useRouter();
const errorMessage = ref('');

onMounted(async () => {
  try {
    await authStore.fetchMyPage();
  } catch (error) {
    errorMessage.value = getApiError(error).message;
  }
});

const handleLogout = async () => {
  errorMessage.value = '';

  try {
    await authStore.logout();
    await router.replace({ name: 'login' });
  } catch (error) {
    errorMessage.value = getApiError(error).message;
  }
};
</script>

<template>
  <main class="p-4">
    <h1 class="h3 mb-4">마이페이지</h1>

    <p v-if="authStore.myPageLoading">회원정보를 불러오는 중입니다.</p>
    <p v-else-if="errorMessage" class="text-danger">{{ errorMessage }}</p>

    <section v-else-if="authStore.myPage">
      <h2 class="h5">회원 정보</h2>
      <p>이메일: {{ authStore.myPage.email }}</p>
      <p>닉네임: {{ authStore.myPage.nickname }}</p>
      <p>캐릭터: {{ authStore.myPage.characterType }}</p>

      <button class="btn btn-outline-secondary" :disabled="authStore.loading" @click="handleLogout">
        로그아웃
      </button>
    </section>
  </main>
</template>
