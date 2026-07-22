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

// 401 응답에서는 사용할 수 없는 토큰을 제거하고 앱에 인증 만료를 알린다.
// 이번 명세에는 Refresh Token이 없으므로 토큰 재발급은 시도하지 않는다.
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
