import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

import { signup as signupApi } from '@/api/auth';
import { getAccessToken, removeAccessToken } from '@/api/token';
import { getMyPage } from '@/api/user';

export const useAuthStore = defineStore('auth', () => {
  const loading = ref(false);
  const myPageLoading = ref(false);
  const accessToken = ref(getAccessToken());
  const myPage = ref(null);

  // 현재는 로그인 기능이 없어 localStorage에 저장된 토큰 존재 여부만 확인한다.
  const isAuthenticated = computed(() => Boolean(accessToken.value));

  const signup = async (signupData) => {
    loading.value = true;

    try {
      const result = await signupApi(signupData);

      return result.data.user;
    } finally {
      loading.value = false;
    }
  };

  const fetchMyPage = async () => {
    myPageLoading.value = true;

    try {
      const result = await getMyPage();

      // 명세에 정의된 마이페이지 요약 데이터는 응답의 data에 위치한다.
      myPage.value = result.data;
      return myPage.value;
    } finally {
      myPageLoading.value = false;
    }
  };

  // 인증 실패 시 Pinia 상태와 localStorage의 인증 정보를 함께 초기화한다.
  const clearAuth = () => {
    removeAccessToken();
    accessToken.value = null;
    myPage.value = null;
  };

  return {
    loading,
    myPageLoading,
    myPage,
    isAuthenticated,
    signup,
    fetchMyPage,
    clearAuth,
  };
});
