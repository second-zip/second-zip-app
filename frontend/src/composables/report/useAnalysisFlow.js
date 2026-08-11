import { computed, ref } from 'vue';

import { getApiError } from '@/api/utils/error';
import {
  ANALYSIS_STEP_FAILURE_MESSAGES,
  ANALYSIS_STEP_MESSAGES,
  AUTH_POLL_INTERVAL_MS,
  DEMO_FALLBACK_REPORT_ID,
  DEMO_PROGRESS_INTERVAL_MS,
  TOTAL_ANALYSIS_STEPS,
} from '@/constants/report/analysisFlow';
import { executeAnalysisFlow } from '@/services/report/analysisFlow';
import { wait } from '@/utils/report/analysisFlow';

let activeAnalysisPromise = null;

export { DEMO_FALLBACK_REPORT_ID } from '@/constants/report/analysisFlow';

export const useAnalysisFlow = ({
  analysisRequest,
  authPollIntervalMs = AUTH_POLL_INTERVAL_MS,
  demoProgressIntervalMs = DEMO_PROGRESS_INTERVAL_MS,
} = {}) => {
  const completedSteps = ref(0);
  const currentStep = ref(1);
  const analysisStatus = ref('idle');
  const failedStep = ref(null);
  const errorMessage = ref('');
  const requestId = ref(null);
  const analysisReportId = ref(null);
  const analysisResult = ref(null);
  const specialTermsResult = ref(null);
  let analysisPromise = null;

  const progress = computed(
    () => (completedSteps.value / TOTAL_ANALYSIS_STEPS) * 100,
  );
  const currentMessage = computed(
    () => ANALYSIS_STEP_MESSAGES[currentStep.value - 1] ?? '',
  );

  const completeDemoFallback = async (error) => {
    const apiError = getApiError(error ?? {});
    failedStep.value = currentStep.value;
    errorMessage.value =
      error?.userMessage ||
      (apiError.code === 'UNKNOWN_ERROR'
        ? ANALYSIS_STEP_FAILURE_MESSAGES[currentStep.value]
        : apiError.message);

    for (let step = completedSteps.value + 1; step <= 6; step += 1) {
      currentStep.value = step;
      await wait(demoProgressIntervalMs);
      completedSteps.value = step;
    }

    analysisReportId.value = DEMO_FALLBACK_REPORT_ID;
    analysisStatus.value = 'success';
    return {
      analysisReportId: DEMO_FALLBACK_REPORT_ID,
      analysisResult: null,
      specialTermsResult: null,
    };
  };

  const executeAnalysis = async () => {
    analysisStatus.value = 'running';
    failedStep.value = null;
    errorMessage.value = '';

    try {
      const result = await executeAnalysisFlow({
        analysisRequest,
        authPollIntervalMs,
        onStepStart: (step) => {
          currentStep.value = step;
        },
        onStepComplete: (step) => {
          completedSteps.value = step;
        },
      });

      requestId.value = result.requestId;
      analysisReportId.value = result.analysisReportId;
      analysisResult.value = result.analysisResult;
      specialTermsResult.value = result.specialTermsResult;
      analysisStatus.value = 'success';
      return result;
    } catch (error) {
      return completeDemoFallback(error);
    }
  };

  const runAnalysis = () => {
    if (analysisPromise) return analysisPromise;
    if (activeAnalysisPromise) return activeAnalysisPromise;

    analysisPromise = executeAnalysis();
    activeAnalysisPromise = analysisPromise;
    analysisPromise.finally(() => {
      if (activeAnalysisPromise === analysisPromise) activeAnalysisPromise = null;
    });
    return analysisPromise;
  };

  return {
    analysisResult,
    analysisReportId,
    analysisStatus,
    completedSteps,
    currentMessage,
    currentStep,
    errorMessage,
    failedStep,
    progress,
    requestId,
    runAnalysis,
    specialTermsResult,
    totalSteps: TOTAL_ANALYSIS_STEPS,
  };
};
