<script setup>
import MainIllust from '@/assets/images/main-illust.png';

defineProps({
  completedSteps: {
    type: Number,
    required: true,
  },
  totalSteps: {
    type: Number,
    required: true,
  },
  progress: {
    type: Number,
    required: true,
  },
  currentMessage: {
    type: String,
    default: '',
  },
  status: {
    type: String,
    required: true,
  },
  errorMessage: {
    type: String,
    default: '',
  },
});
</script>

<template>
  <section
    class="analysis-progress d-flex flex-column align-items-center justify-content-center text-center"
  >
    <img
      :src="MainIllust"
      class="analysis-progress__image"
      alt="집 분석 안내 캐릭터"
    />

    <template v-if="status === 'failed'">
      <h1 class="analysis-progress__title fw-bold mb-2">
        분석을 완료하지 못했어요.
      </h1>
      <p class="analysis-progress__description mb-1">
        잠시 후 다시 시도해주세요.
      </p>
      <p class="analysis-progress__message mb-0" role="alert">
        {{ errorMessage }}
      </p>
    </template>

    <template v-else>
      <h1 class="analysis-progress__title fw-bold mb-2">
        집을 분석하고 있어요
      </h1>
      <p class="analysis-progress__description mb-1">
        잠시만 기다려 주세요.
      </p>
      <p class="analysis-progress__message mb-0" role="status">
        {{ currentMessage }}
      </p>
    </template>

    <div
      class="analysis-progress__track w-100"
      role="progressbar"
      aria-label="리포트 분석 진행률"
      aria-valuemin="0"
      :aria-valuemax="totalSteps"
      :aria-valuenow="completedSteps"
    >
      <div
        class="analysis-progress__bar"
        :style="{ width: `${progress}%` }"
      />
    </div>

    <p class="analysis-progress__count fw-semibold mb-0">
      {{ completedSteps }} / {{ totalSteps }}
    </p>
  </section>
</template>

<style scoped src="./AnalysisProgress.css" />
