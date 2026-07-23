import api from './instance';

// 회원가입 데이터를 백엔드에 전달한다.
// JSON Content-Type은 Axios가 요청 본문에 맞게 자동으로 설정한다.
export const signup = async (signupData) => {
  const response = await api.post('/auth/signup', signupData);

  return response.data;
};
