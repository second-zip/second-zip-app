export const getApiError = (error) => {
  const responseData = error.response?.data;
  const isTimeout = error.code === 'ECONNABORTED';

  // 백엔드 오류 응답에서 화면에 필요한 코드와 메시지만 공통 형태로 추출한다.
  // 응답이 없는 네트워크 오류에도 화면이 사용할 기본값을 제공한다.
  return {
    code:
      responseData?.code ??
      responseData?.error ??
      error.code ??
      'UNKNOWN_ERROR',
    message:
      responseData?.message ??
      (isTimeout
        ? '요청 시간이 초과되었습니다. 다시 시도해주세요.'
        : '요청을 처리하는 중 오류가 발생했습니다.'),
  };
};

// 화면에서 오류 메시지만 필요한 경우 사용하는 편의 함수다.
export const getApiErrorMessage = (error) => {
  return getApiError(error).message;
};
