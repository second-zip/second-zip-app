import axios from 'axios';

import {
  getAccessToken,
  getRefreshToken,
  removeAccessToken,
  removeRefreshToken,
  setAccessToken,
} from './token';
import { reissueAccessToken } from './tokenReissue';

export const AUTH_UNAUTHORIZED_EVENT = 'auth:unauthorized';

// 모든 API 요청에서 공통으로 사용할 서버 주소와 기본 옵션을 설정한다.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    Accept: 'application/json',
  },
});

const TOKEN_REFRESH_EXCLUDED_PATHS = new Set([
  '/auth/login',
  '/auth/signup',
  '/auth/token/reissue',
]);
let refreshPromise = null;

const notifyUnauthorized = () => {
  removeAccessToken();
  removeRefreshToken();
  window.dispatchEvent(new CustomEvent(AUTH_UNAUTHORIZED_EVENT));
};

const requestNewAccessToken = () => {
  if (refreshPromise) return refreshPromise;

  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    notifyUnauthorized();
    return Promise.reject(new Error('Refresh Token이 없습니다.'));
  }

  refreshPromise = reissueAccessToken(refreshToken)
    .then((data) => {
      const accessToken = data?.accessToken;
      if (!accessToken) throw new Error('재발급 응답에 Access Token이 없습니다.');

      setAccessToken(accessToken);
      return accessToken;
    })
    .catch((error) => {
      notifyUnauthorized();
      throw error;
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
};

const setAuthorizationHeader = (config, accessToken) => {
  if (typeof config.headers?.set === 'function') {
    config.headers.set('Authorization', `Bearer ${accessToken}`);
    return;
  }

  config.headers = {
    ...config.headers,
    Authorization: `Bearer ${accessToken}`,
  };
};

// 저장된 Access Token이 있을 때만 JWT 인증 헤더를 요청에 추가한다.
api.interceptors.request.use((config) => {
  const accessToken = getAccessToken();

  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`);
  }

  return config;
});

// Access Token 만료 시 한 번만 재발급하고 실패했던 원 요청을 새 토큰으로 재시도한다.
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const isRefreshExcluded = TOKEN_REFRESH_EXCLUDED_PATHS.has(
      originalRequest?.url,
    );

    if (error.response?.status !== 401 || isRefreshExcluded) {
      return Promise.reject(error);
    }

    if (!originalRequest || originalRequest._retry) {
      notifyUnauthorized();
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    try {
      const accessToken = await requestNewAccessToken();
      setAuthorizationHeader(originalRequest, accessToken);
      return api.request(originalRequest);
    } catch {
      return Promise.reject(error);
    }
  },
);

export default api;
