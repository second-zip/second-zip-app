<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getApiError } from '@/api/utils/error';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const router = useRouter();
const errorMessage = ref('');
const characterSaving = ref(false);
const characterOptions = [
  { value: 'WOMAN', label: '여자' },
  { value: 'MAN', label: '남자' },
  { value: 'CAT', label: '고양이' },
];

onMounted(async () => {
  try {
    await authStore.fetchMyPage();
  } catch (error) {
    errorMessage.value = getApiError(error).message;
  }
});

// 로그아웃 후 로그인 화면으로 이동
const handleLogout = async () => {
  errorMessage.value = '';

  try {
    await authStore.logout();
    await router.replace({ name: 'login' });
  } catch (error) {
    errorMessage.value = getApiError(error).message;
  }
};

const handleCharacterChange = async (characterType) => {
  if (characterSaving.value || authStore.myPage?.characterType === characterType) {
    return;
  }

  errorMessage.value = '';
  characterSaving.value = true;

  try {
    await authStore.changeCharacter(characterType);
  } catch (error) {
    errorMessage.value = getApiError(error).message;
  } finally {
    characterSaving.value = false;
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

      <fieldset id="ai-secretary" class="mb-4" :disabled="characterSaving">
        <legend class="h6">AI 비서 선택하기</legend>
        <div class="btn-group" role="group" aria-label="캐릭터 선택">
          <button
            v-for="option in characterOptions"
            :key="option.value"
            type="button"
            class="btn"
            :class="authStore.myPage.characterType === option.value
              ? 'btn-primary'
              : 'btn-outline-primary'"
            :aria-pressed="authStore.myPage.characterType === option.value"
            @click="handleCharacterChange(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
        <p v-if="characterSaving" class="mt-2 mb-0">변경 중...</p>
      </fieldset>

      <button
        class="btn btn-outline-secondary"
        :disabled="authStore.loading"
        @click="handleLogout"
      >
        로그아웃
      </button>
    </section>
  </main>
</template>
