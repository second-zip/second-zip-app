<script setup>
import MicrophoneIcon from '@/assets/icons/checklist/microphone-mint-18.svg';

defineProps({ starting: { type: Boolean, default: false } });
defineEmits(['start']);
</script>

<template>
  <div class="recording-idle d-flex align-items-center gap-2">
    <div
      class="recording-idle__icon d-flex flex-shrink-0 align-items-center justify-content-center"
    >
      <img :src="MicrophoneIcon" alt="" />
    </div>
    <p class="recording-idle__description flex-grow-1 mb-0">
      녹음으로 계약 내용을 확인해 보세요.
    </p>
    <button
      type="button"
      class="recording-idle__start flex-shrink-0 fw-semibold"
      :disabled="starting"
      @click="$emit('start')"
    >{{ starting ? '준비 중' : '녹음 시작' }}</button>
  </div>
</template>

<style scoped>
.recording-idle__icon {
  width: 32px;
  height: 32px;
  background-color: var(--mint-100);
  border-radius: 11px;
}

.recording-idle__icon img { width: 18px; height: 18px; }
.recording-idle__description {
  min-width: 0;
  color: var(--black-500);
  font-size: 0.75rem;
  line-height: 1.35;
}

.recording-idle__start {
  height: 30px;
  padding: 0 11px;
  color: var(--black-700);
  white-space: nowrap;
  background-color: var(--mint-100);
  border: 1px solid var(--mint-500);
  border-radius: 10px;
  font-size: 0.6875rem;
}

.recording-idle__start:hover {
  color: var(--black-900);
  background-color: var(--mint-500);
}

.recording-idle__start:disabled { opacity: 0.55; }
</style>
