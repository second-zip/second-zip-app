import api from './instance';

// Swagger 기준: 내 회원정보 조회
export const getMyAccount = async () => {
  const response = await api.get('/user');

  return response.data;
};

// Swagger 기준: 내 회원정보 수정
export const updateMyAccount = async (accountData) => {
  const response = await api.patch('/user', accountData);

  return response.data;
};

// Swagger 기준: 회원 탈퇴
export const withdraw = async (withdrawData) => {
  const response = await api.delete('/user', {
    data: withdrawData,
  });

  return response.data;
};

// Swagger 기준: 캐릭터 유형 변경
export const updateCharacter = async (characterData) => {
  const response = await api.patch('/user/character', characterData);

  return response.data;
};

// Swagger 기준: 비밀번호 변경
export const updatePassword = async (passwordData) => {
  const response = await api.patch('/user/password', passwordData);

  return response.data;
};
