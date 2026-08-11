<script setup>
import { useRouter } from 'vue-router';

import ReportCreateForm from '@/components/report/create/ReportCreateForm.vue';

const router = useRouter();

let isOpeningAnalysis = false;

const appendAddressUnit = (value = '', unit) => {
  const normalizedValue = value.trim();

  if (!normalizedValue) return '';
  return normalizedValue.endsWith(unit)
    ? normalizedValue
    : `${normalizedValue}${unit}`;
};

const createAnalysisRequest = (formData) => ({
  roadAddress: String(
    formData.address?.roadAddress ||
      formData.address?.jibunAddress ||
      formData.addressKeyword ||
      '',
  ).trim(),
  detailAddress: [
    appendAddressUnit(formData.dong, '동'),
    appendAddressUnit(formData.ho, '호'),
  ]
    .filter(Boolean)
    .join(' '),
  deposit: formData.deposit * 10_000,
});

const handleSubmit = async (formData) => {
  if (!formData.validateReport) return;

  if (isOpeningAnalysis) return;

  isOpeningAnalysis = true;

  try {
    await router.push({
      name: 'analysis-progress',
      state: {
        analysisRequest: createAnalysisRequest(formData),
      },
    });
  } finally {
    isOpeningAnalysis = false;
  }
};
</script>

<template>
  <div class="report-create-view d-flex flex-column">
    <div class="report-create-view__body flex-grow-1">
      <ReportCreateForm @submit="handleSubmit" />
    </div>
  </div>
</template>

<style scoped>
.report-create-view {
  width: 100%;
  height: 100%;
  padding: 20px;
  overflow: hidden;
  box-sizing: border-box;
  background-color: #fff;
}

.report-create-view__header {
  height: 56px;
}

.report-create-view__header h1 {
  color: var(--black-300);
  font-size: 0.875rem;
}

.report-create-view__body {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: none;
}

.report-create-view__body::-webkit-scrollbar {
  display: none;
}
</style>
