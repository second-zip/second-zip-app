import api from './instance';

// 세진: 로그인한 사용자의 회원정보를 조회한다.
export const getMyAccount = async () => {
  const response = await api.get('/user');

  return response.data;
};
