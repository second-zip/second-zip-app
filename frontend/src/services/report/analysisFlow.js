import {
  completeAnalysis,
  createAnalysisRequest,
  createSpecialTerms,
  getExternalReadiness,
  startAnalysisAuth,
} from '@/api/analysisReport';
import { finishAnalysisAuthentication } from '@/services/report/analysisAuth';
import {
  createAnalysisFlowError,
  getAnalysisAuthPayload,
  normalizeAnalysisRequest,
  requireResponseValue,
} from '@/utils/report/analysisFlow';

export const executeAnalysisFlow = async ({
  analysisRequest,
  authPollIntervalMs,
  onStepStart,
  onStepComplete,
}) => {
  onStepStart(1);
  const readiness = await getExternalReadiness();
  if (readiness?.ready !== true) {
    throw createAnalysisFlowError('분석 환경이 준비되지 않았습니다.');
  }
  onStepComplete(1);

  onStepStart(2);
  const requestPayload = normalizeAnalysisRequest(analysisRequest);
  const requestResponse = await createAnalysisRequest(requestPayload);
  const requestId = requireResponseValue(
    requestResponse?.requestId,
    '분석 요청 정보를 확인하지 못했습니다.',
  );
  const authSelectionRequest = {
    ...requestPayload,
    roadAddress: requestResponse?.roadAddress ?? '',
  };
  onStepComplete(2);

  onStepStart(3);
  const authPayload = getAnalysisAuthPayload();
  const startAuthResponse = await startAnalysisAuth(requestId, authPayload);
  onStepComplete(3);

  onStepStart(4);
  await finishAnalysisAuthentication({
    initialResponse: startAuthResponse,
    requestId,
    authPayload,
    analysisRequest: authSelectionRequest,
    pollIntervalMs: authPollIntervalMs,
  });
  onStepComplete(4);

  onStepStart(5);
  const analysisResult = await completeAnalysis(requestId);
  const analysisReportId = requireResponseValue(
    analysisResult?.analysisReportId,
    '분석 결과 정보를 확인하지 못했습니다.',
  );
  onStepComplete(5);

  onStepStart(6);
  const specialTermsResult = await createSpecialTerms(analysisReportId);
  onStepComplete(6);

  return {
    requestId,
    analysisReportId,
    analysisResult,
    specialTermsResult,
  };
};
