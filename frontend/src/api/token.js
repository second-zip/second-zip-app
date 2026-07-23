const ACCESS_TOKEN_KEY = 'accessToken';

// 로그인 기능이 추가되면 같은 키를 사용해 발급받은 Access Token을 저장한다.
export const getAccessToken = () => {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
};

export const setAccessToken = (accessToken) => {
  if (accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  }
};

export const removeAccessToken = () => {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
};
