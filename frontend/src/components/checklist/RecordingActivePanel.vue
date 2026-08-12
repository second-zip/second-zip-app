<script setup>
import { formatAudioTime } from '@/utils/audio';
import RecordingWaveform from './RecordingWaveform.vue';

defineProps({
  elapsedSeconds: { type: Number, default: 0 },
  levels: { type: Array, default: () => [] },
  finishing: { type: Boolean, default: false },
});
defineEmits(['finish']);
</script>

<template>
  <div class="recording-active d-flex flex-column">
    <div class="d-flex align-items-center gap-2 w-100">
      <div class="d-flex flex-shrink-0 align-items-center gap-2">
        <span class="recording-active__indicator" aria-hidden="true"></span>
        <span class="recording-active__timer fw-semibold">
          {{ formatAudioTime(elapsedSeconds) }}
        </span>
      </div>
      <RecordingWaveform class="flex-grow-1" :levels="levels" />
      <button
        type="button"
        class="recording-active__stop flex-shrink-0 border-0 fw-semibold"
        :disabled="finishing"
        @click="$emit('finish')"
      >{{ finishing ? '저장 중' : '녹음 종료' }}</button>
    </div>
    <p class="recording-active__guide mt-2 mb-0 text-center">
      녹음을 마치면 대화 내용을 분석해 체크리스트에 반영해 드려요.
    </p>
  </div>
</template>

<style scoped>
.recording-active__indicator {
  width: 8px;
  height: 8px;
  background-color: var(--red-500);
  border-radius: 50%;
  animation: recording-pulse 1.2s ease-in-out infinite;
}

.recording-active__timer {
  color: var(--black-700);
  font-size: 0.75rem;
  font-variant-numeric: tabular-nums;
}

.recording-active__stop {
  height: 30px;
  padding: 0 11px;
  color: var(--black-900);
  white-space: nowrap;
  background-color: var(--mint-500);
  border-radius: 999px;
  font-size: 0.6875rem;
}

.recording-active__stop:disabled { opacity: 0.55; }
.recording-active__guide {
  color: var(--black-300);
  font-size: 0.6875rem;
  line-height: 1.4;
}

@keyframes recording-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}
</style>
