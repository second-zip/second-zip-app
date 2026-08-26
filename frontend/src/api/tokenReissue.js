import axios from 'axios';

// 재발급 요청은 일반 API 인터셉터를 거치지 않아야 401 재귀 호출을 막을 수 있다.
const tokenReissueApi = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    Accept: 'application/json',
  },
});

export const reissueAccessToken = async (refreshToken) => {
  const response = await tokenReissueApi.post('/auth/token/reissue', {
    refreshToken,
  });

  return response.data;
};
