import api from './instance';

// API 명세서 기준: 주소 키워드 검색
export const searchAddresses = async (keyword) => {
  const response = await api.get('/address/search', {
    params: {
      keyword,
    },
  });

  return response.data;
};
