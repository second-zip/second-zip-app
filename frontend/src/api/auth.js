import api from './instance';

export const signup = async (signupData) => {
  const response = await api.post('/auth/signup', signupData);

  return response.data;
};

// 세진: 이메일과 비밀번호로 로그인하고 JWT 토큰을 발급받는다.
export const login = async (loginData) => {
  const response = await api.post('/auth/login', loginData);

  return response.data;
};

// 세진: 현재 Access Token을 전달해 서버 로그아웃을 처리한다.
export const logout = async () => {
  const response = await api.post('/auth/logout');

  return response.data;
};
