import api from './instance';

// API 명세서 기준: 개인정보 동의 상태 조회
export const getConsents = async () => {
  const response = await api.get('/terms/consents');

  return response.data;
};

// API 명세서 기준: 개인정보 선택 동의 변경
export const updateConsents = async (consentData) => {
  const response = await api.patch('/terms/consents', consentData);

  return response.data;
};

// API 명세서 기준: 최신 이용약관 및 고지 조회
export const getLatestTerms = async () => {
  const response = await api.get('/terms/latest');

  return response.data;
};

// API 명세서 기준: 서비스 이용약관 확인 처리
export const confirmTerms = async (termsId) => {
  const response = await api.patch(`/terms/${termsId}/confirm`);

  return response.data;
};
