import api from './instance';

// JWT에서 식별된 사용자의 마이페이지 요약 정보를 조회한다.
export const getMyPage = async () => {
  const response = await api.get('/users/me/mypage');

  return response.data;
};
