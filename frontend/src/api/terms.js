import api from './instance';

// API 명세서 기준: 개인정보 동의 상태 조회
export const getConsents = async () => {
  const response = await api.get('/terms/consents');

  return response.data;
};

// API 명세서 기준: 최신 이용약관 및 고지 조회
export const getLatestTerms = async () => {
  const response = await api.get('/terms/latest');

  return response.data;
};
