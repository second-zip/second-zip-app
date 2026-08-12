export const createAnalysisFlowError = (message) => {
  const error = new Error(message);
  error.userMessage = message;
  return error;
};

export const requireResponseValue = (value, message) => {
  if (value === null || value === undefined || value === '') {
    throw createAnalysisFlowError(message);
  }

  return value;
};

export const wait = (delay) =>
  delay > 0
    ? new Promise((resolve) => globalThis.setTimeout(resolve, delay))
    : Promise.resolve();

export const getAnalysisAuthPayload = () => {
  const authPayload = {
    birthDate: import.meta.env.VITE_ANALYSIS_TEST_BIRTH_DATE?.trim() ?? '',
    consent: true,
    phoneNo: import.meta.env.VITE_ANALYSIS_TEST_PHONE_NO?.trim() ?? '',
    provider: 'KAKAO',
    telecom: 'SKT',
    userName: import.meta.env.VITE_ANALYSIS_TEST_USER_NAME?.trim() ?? '',
  };

  if (!authPayload.birthDate || !authPayload.phoneNo || !authPayload.userName) {
    throw createAnalysisFlowError('분석 인증 설정을 확인해주세요.');
  }

  return authPayload;
};

export const normalizeAnalysisRequest = (analysisRequest) => {
  const payload = {
    roadAddress: analysisRequest?.roadAddress?.trim() ?? '',
    detailAddress: analysisRequest?.detailAddress?.trim() ?? '',
    deposit: Number(analysisRequest?.deposit),
  };

  if (!payload.roadAddress || !Number.isFinite(payload.deposit)) {
    throw createAnalysisFlowError('입력한 분석 정보를 확인해주세요.');
  }

  return payload;
};
