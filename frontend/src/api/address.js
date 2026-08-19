import api from './instance';

export const searchAddresses = async (query) => {
  const keyword = query.trim();

  if (!keyword) {
    return [];
  }

  const response = await api.get('/addresses', {
    params: { query: keyword },
  });

  return Array.isArray(response.data?.addresses) ? response.data.addresses : [];
};
