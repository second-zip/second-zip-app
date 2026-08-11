<script setup>
import { onBeforeUnmount, onMounted, toRaw } from 'vue';
import { useRouter } from 'vue-router';

import AnalysisProgress from '@/components/report/analysis/AnalysisProgress.vue';
import { useAnalysisFlow } from '@/composables/report/useAnalysisFlow';

const router = useRouter();
const analysisFlow = useAnalysisFlow({
  analysisRequest: window.history.state?.analysisRequest ?? null,
});

let isViewActive = true;

onMounted(async () => {
  const result = await analysisFlow.runAnalysis();

  if (!result || !isViewActive) return;

  await router.replace({
    name: 'analysis',
    params: {
      analysisReportId: result.analysisReportId,
    },
    state: {
      analysisResult: toRaw(result.analysisResult),
      specialTermsResult: toRaw(result.specialTermsResult),
    },
  });
});

onBeforeUnmount(() => {
  isViewActive = false;
});
</script>

<template>
  <div class="analysis-progress-view">
    <AnalysisProgress
      :completed-steps="analysisFlow.completedSteps.value"
      :total-steps="analysisFlow.totalSteps"
      :progress="analysisFlow.progress.value"
      :current-message="analysisFlow.currentMessage.value"
      :status="analysisFlow.analysisStatus.value"
      :error-message="analysisFlow.errorMessage.value"
    />
  </div>
</template>

<style scoped>
.analysis-progress-view {
  width: 100%;
  height: 100%;
  background-color: #fff;
}
</style>
