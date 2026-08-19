import axios from 'axios';

import { getAccessToken, removeAccessToken } from './token';

export const AUTH_UNAUTHORIZED_EVENT = 'auth:unauthorized';

// 모든 API 요청에서 공통으로 사용할 서버 주소와 기본 옵션을 설정한다.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    Accept: 'application/json',
  },
});

// 저장된 Access Token이 있을 때만 JWT 인증 헤더를 요청에 추가한다.
api.interceptors.request.use((config) => {
  const accessToken = getAccessToken();

  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`);
  }

  return config;
});

// 재발급 API가 없으므로 401 응답에서는 인증 정보를 정리하고 로그인으로 이동한다.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      removeAccessToken();
      window.dispatchEvent(new CustomEvent(AUTH_UNAUTHORIZED_EVENT));
    }

    return Promise.reject(error);
  },
);

export default api;
