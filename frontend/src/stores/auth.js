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

  const fetchMyPage = async () => {
    myPageLoading.value = true;

    try {
      myPage.value = await getMyAccount();

      return myPage.value;
    } finally {
      myPageLoading.value = false;
    }
  };

  const clearAuth = () => {
    removeAccessToken();
    accessToken.value = null;
    myPage.value = null;
  };

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
