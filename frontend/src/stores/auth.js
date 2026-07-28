import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

import {
  login as loginApi,
  logout as logoutApi,
  signup as signupApi,
} from '@/api/auth';
import {
  getAccessToken,
  removeAccessToken,
  setAccessToken,
} from '@/api/token';
import { getMyAccount } from '@/api/user';

export const useAuthStore = defineStore('auth', () => {
  const loading = ref(false);
  const myPageLoading = ref(false);
  const accessToken = ref(getAccessToken());
  const myPage = ref(null);
  const isAuthenticated = computed(() => Boolean(accessToken.value));

  const signup = async (signupData) => {
    loading.value = true;

    try {
      return await signupApi(signupData);
    } finally {
      loading.value = false;
    }
  };

  // 세진: 로그인 성공 후 토큰과 사용자 정보를 Pinia 상태에 저장한다.
  const login = async (loginData) => {
    loading.value = true;

    try {
      const result = await loginApi(loginData);

      setAccessToken(result.accessToken);
      accessToken.value = result.accessToken;
      myPage.value = {
        accountId: result.accountId,
        characterType: result.characterType,
        email: result.email,
        nickname: result.nickname,
      };

      return myPage.value;
    } finally {
      loading.value = false;
    }
  };

  // 세진: 저장된 토큰으로 로그인 사용자의 최신 회원정보를 불러온다.
  const fetchMyPage = async () => {
    myPageLoading.value = true;

    try {
      myPage.value = await getMyAccount();

      return myPage.value;
    } finally {
      myPageLoading.value = false;
    }
  };

  // 세진: 브라우저 토큰과 Pinia의 인증 정보를 함께 초기화한다.
  const clearAuth = () => {
    removeAccessToken();
    accessToken.value = null;
    myPage.value = null;
  };

  // 세진: 서버 로그아웃 후 클라이언트 인증 정보를 제거한다.
  const logout = async () => {
    loading.value = true;

    try {
      await logoutApi();
    } finally {
      clearAuth();
      loading.value = false;
    }
  };

  return {
    loading,
    myPageLoading,
    myPage,
    isAuthenticated,
    signup,
    login,
    logout,
    fetchMyPage,
    clearAuth,
  };
});
