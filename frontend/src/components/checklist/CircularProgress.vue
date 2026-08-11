<script setup>
import { computed } from 'vue';

import ProgressBlueIcon from '@/assets/icons/checklist/progress-blue-20.svg';
import ProgressGrayIcon from '@/assets/icons/checklist/progress-gray-20.svg';

const props = defineProps({
  value: {
    type: Number,
    default: 0,
  },
  size: {
    type: Number,
    default: 20,
  },
});

const normalizedValue = computed(() =>
  Math.min(100, Math.max(0, Number(props.value) || 0)),
);
const progressStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  '--progress-angle': `${normalizedValue.value * 3.6}deg`,
}));
</script>

<template>
  <div
    class="circular-progress position-relative flex-shrink-0"
    :style="progressStyle"
    role="progressbar"
    aria-label="체크리스트 생성 진행률"
    aria-valuemin="0"
    aria-valuemax="100"
    :aria-valuenow="normalizedValue"
  >
    <img
      :src="ProgressGrayIcon"
      class="circular-progress__track position-absolute w-100 h-100"
      alt=""
    />
    <img
      :src="ProgressBlueIcon"
      class="circular-progress__value position-absolute w-100 h-100"
      alt=""
    />
  </div>
</template>

<style scoped>
.circular-progress__track,
.circular-progress__value {
  inset: 0;
}

.circular-progress__value {
  mask-image: conic-gradient(
    black 0deg var(--progress-angle),
    transparent var(--progress-angle) 360deg
  );
  mask-size: 100% 100%;
  -webkit-mask-image: conic-gradient(
    black 0deg var(--progress-angle),
    transparent var(--progress-angle) 360deg
  );
  -webkit-mask-size: 100% 100%;
}
</style>
