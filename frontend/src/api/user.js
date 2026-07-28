import api from './instance';

export const getMyAccount = async () => {
  const response = await api.get('/user');

  return response.data;
};
