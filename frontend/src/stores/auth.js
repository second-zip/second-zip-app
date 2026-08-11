import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

import {
  login as loginApi,
  logout as logoutApi,
  signup as signupApi,
} from '@/api/auth';
import { getAccessToken, removeAccessToken, setAccessToken } from '@/api/token';
import {
  getMyAccount,
  updateCharacter as updateCharacterApi,
  updateMyAccount,
  updatePassword as updatePasswordApi,
  withdraw as withdrawApi,
} from '@/api/user';

export const useAuthStore = defineStore('auth', () => {
  const loading = ref(false);
  const myPageLoading = ref(false);
  const accessToken = ref(getAccessToken());
  const myPage = ref(null);
  const isAuthenticated = computed(() => Boolean(accessToken.value));
  const characterType = computed(() => myPage.value?.characterType ?? 'CAT');

  const signup = async (signupData) => {
    loading.value = true;

    try {
      return await signupApi(signupData);
    } finally {
      loading.value = false;
    }
  };

  // 로그인 성공 후 토큰과 사용자 정보를 Pinia 상태에 저장
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

  // 저장된 토큰으로 로그인 사용자의 최신 회원정보 불러오기
  const fetchMyPage = async () => {
    myPageLoading.value = true;

    try {
      myPage.value = await getMyAccount();

      return myPage.value;
    } finally {
      myPageLoading.value = false;
    }
  };

  const changeCharacter = async (characterType) => {
    const updatedAccount = await updateCharacterApi({ characterType });

    myPage.value = updatedAccount;

    return updatedAccount;
  };

  const updateProfile = async (nickname) => {
    const updatedAccount = await updateMyAccount({ nickname });
    myPage.value = updatedAccount;
    return updatedAccount;
  };

  const withdraw = async (password) => {
    loading.value = true;
    try {
      const result = await withdrawApi({ password });
      clearAuth();
      return result;
    } finally {
      loading.value = false;
    }
  };

  const changePassword = async (passwordData) => {
    loading.value = true;
    try {
      const result = await updatePasswordApi(passwordData);
      // 백엔드에서 비밀번호 변경과 동시에 기존 토큰을 무효화합니다.
      clearAuth();
      return result;
    } finally {
      loading.value = false;
    }
  };

  // 브라우저 토큰과 Pinia의 인증 정보를 함께 초기화
  const clearAuth = () => {
    removeAccessToken();
    accessToken.value = null;
    myPage.value = null;
  };

  // 서버 로그아웃 후 클라이언트 인증 정보를 제거
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
    characterType,
    isAuthenticated,
    signup,
    login,
    logout,
    fetchMyPage,
    changeCharacter,
    updateProfile,
    changePassword,
    withdraw,
    clearAuth,
  };
});
