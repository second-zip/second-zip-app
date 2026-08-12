import {
  continueAnalysisAuth,
  getAnalysisRequest,
  startAnalysisAuth,
} from '@/api/analysisReport';
import { MAX_AUTH_TRANSITIONS } from '@/constants/report/analysisFlow';
import {
  getContinuePayload,
  isRecoverableAuthError,
} from '@/utils/report/analysisAuth';
import { createAnalysisFlowError, wait } from '@/utils/report/analysisFlow';

const continueAuthentication = async (requestId, payload, intervalMs) => {
  try {
    return await continueAnalysisAuth(requestId, payload);
  } catch (error) {
    if (!isRecoverableAuthError(error)) throw error;

    await wait(intervalMs);

    try {
      return await getAnalysisRequest(requestId);
    } catch {
      throw error;
    }
  }
};

export const finishAnalysisAuthentication = async ({
  initialResponse,
  requestId,
  authPayload,
  analysisRequest,
  pollIntervalMs,
}) => {
  let authResponse = initialResponse;

  for (let transition = 0; transition < MAX_AUTH_TRANSITIONS; transition += 1) {
    if (authResponse?.status === 'PROCESSING') return authResponse;

    if (authResponse?.status === 'AUTH_REQUIRED') {
      authResponse = await startAnalysisAuth(requestId, authPayload);
      continue;
    }

    const needsContinue =
      authResponse?.status === 'AUTH_PENDING' ||
      authResponse?.status === 'SELECTION_REQUIRED';

    if (!needsContinue) {
      throw createAnalysisFlowError('추가 인증 상태를 확인하지 못했습니다.');
    }

    const payload = getContinuePayload(
      authResponse,
      authPayload,
      analysisRequest,
    );

    if (authResponse.nextAction === 'SIMPLE_AUTH') await wait(pollIntervalMs);
    authResponse = await continueAuthentication(requestId, payload, pollIntervalMs);
  }

  throw createAnalysisFlowError('인증 대기 시간이 초과되었습니다.');
};
