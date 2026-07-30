<!-- 분석 메인(나머지 파츠들은 여기서 조립) -->
<script setup>
import AAnalysis from './A_analysis.vue';
import BAnalysis from './B_analysis.vue';
import CAnalysis from './C_analysis.vue';
import DAnalysis from './D_analysis.vue';
import EAnalysis from './E_analysis.vue';

defineProps({
  overallRisk: { type: String, required: true },
  secretaryImage: { type: String, required: true },
  overallIcon: { type: String, required: true },
  overallMessage: { type: String, required: true },
  formattedDeposit: { type: String, required: true },
  rentRatioDisplay: { type: String, required: true },
  checks: { type: Array, required: true },
  checkRisk: { type: String, required: true },
  fraudTypes: { type: Array, required: true },
  fraudRisk: { type: String, required: true },
  riskIcons: { type: Object, required: true },
  riskLabels: { type: Object, required: true },
  specialTerms: { type: Array, required: true },
  specialTermsNotice: { type: String, required: true },
  defaultSecretaryImage: { type: String, required: true },
});
</script>

<template>
  <main class="analysis-content">
    <!-- 1. 위험도 요약/비서 결과 -->
    <AAnalysis
      :risk="overallRisk"
      :character="secretaryImage"
      :icon="overallIcon"
      :message="overallMessage"
    />

    <!-- 2. 보증금·전세가율 -->
    <BAnalysis :deposit="formattedDeposit" :ratio="rentRatioDisplay" />

    <!-- 3. 필수 점검 -->
    <CAnalysis
      :checks
      :risk="checkRisk"
      :icons="riskIcons"
      :labels="riskLabels"
    />

    <!-- 4. 전세사기 유형·세부 항목 -->
    <DAnalysis
      :fraud-types
      :risk="fraudRisk"
      :icons="riskIcons"
      :labels="riskLabels"
    />

    <!-- 5. AI 추천 특약 -->
    <EAnalysis
      :terms="specialTerms"
      :notice="specialTermsNotice"
      :character="defaultSecretaryImage"
    />
  </main>
</template>

<style scoped>
.analysis-content {
  padding: 0 1.25rem 12rem;
}
</style>
