import api from './instance';

// Swagger 기준: 전세사기 피해 현황 지도 조회
export const getFraudDamages = async () => {
  const response = await api.get('/maps/fraud-damage');

  return response.data;
};

// API 명세서 기준: 지역별 전세가격 변동 조회
export const getJeonsePriceChanges = async (params = {}) => {
  const response = await api.get('/maps/jeonse-price-changes', {
    params,
  });

  return response.data;
};
