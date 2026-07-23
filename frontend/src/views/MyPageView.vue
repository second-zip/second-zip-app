<script setup>
import { onMounted, ref } from 'vue';

import { getApiError } from '@/api/utils/error';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const errorMessage = ref('');

// 화면 진입 시 JWT 인증 사용자 기준의 마이페이지 요약 정보를 조회한다.
onMounted(async () => {
  try {
    await authStore.fetchMyPage();
  } catch (error) {
    errorMessage.value = getApiError(error).message;
  }
});
</script>

<template>
  <main>
    <h1>마이페이지</h1>

    <p v-if="authStore.myPageLoading">마이페이지 정보를 불러오는 중입니다.</p>
    <p v-else-if="errorMessage">{{ errorMessage }}</p>

    <template v-else-if="authStore.myPage">
      <section>
        <h2>회원 정보</h2>
        <p>이메일: {{ authStore.myPage.user.email }}</p>
        <p>닉네임: {{ authStore.myPage.user.nickname }}</p>
        <!-- TODO: 회원정보 수정 화면의 경로와 명세가 확정되면 이동 버튼을 연결한다. -->
      </section>

      <section>
        <h2>알림 설정</h2>
        <p>{{ authStore.myPage.notificationSetting.enabled ? '사용 중' : '사용 안 함' }}</p>
        <!-- TODO: 알림 설정 화면의 경로와 변경 API 명세가 확정되면 이동 버튼을 연결한다. -->
      </section>

      <section>
        <h2>내 집 관리</h2>
        <p>등록된 집: {{ authStore.myPage.houseSummary.houseCount }}개</p>
        <!-- TODO: 내 집 관리 화면 경로가 확정되면 이동 버튼을 연결한다. -->
      </section>

      <section>
        <h2>약관 및 개인정보 동의</h2>
        <p>이용약관: {{ authStore.myPage.consentSummary.termsAgreed ? '동의' : '미동의' }}</p>
        <p>
          개인정보처리방침:
          {{ authStore.myPage.consentSummary.privacyAgreed ? '동의' : '미동의' }}
        </p>
        <!-- TODO: 동의 관리 화면의 경로와 변경 API 명세가 확정되면 이동 버튼을 연결한다. -->
      </section>
    </template>
  </main>
</template>
