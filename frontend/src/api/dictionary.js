import api from './instance';

// API 명세서 기준: 부동산 용어 조회
export const getDictionaryWords = async () => {
  const response = await api.get('/dictionary/word');

  return response.data;
};

// API 명세서 기준: 전세사기 유형 조회
export const getFraudTypes = async () => {
  const response = await api.get('/dictionary/fraud');

  return response.data;
};

// API 명세서 기준: 건축물대장 읽기 가이드 조회
export const getBuildingRegisterGuide = async () => {
  const response = await api.get('/dictionary/read');

  return response.data;
};

// API 명세서 기준: 확정일자 및 전입신고 가이드 조회
export const getChangeAddressGuide = async () => {
  const response = await api.get('/dictionary/change-address');

  return response.data;
};
