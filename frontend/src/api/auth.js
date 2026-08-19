import api from './instance';

// Swagger 기준: 회원가입
export const signup = async (signupData) => {
  const response = await api.post('/auth/signup', signupData);

  return response.data;
};

// Swagger 기준: 로그인
export const login = async (loginData) => {
  const response = await api.post('/auth/login', loginData);

  return response.data;
};

// Swagger 기준: 로그아웃
export const logout = async () => {
  const response = await api.post('/auth/logout');

  return response.data;
};
