<script setup>
import { computed } from 'vue';

import RecordingModalShell from './RecordingModalShell.vue';

const props = defineProps({
  open: { type: Boolean, default: false },
  text: { type: String, default: '' },
});
const emit = defineEmits(['close']);
const hasText = computed(() => Boolean(props.text.trim()));
</script>

<template>
  <RecordingModalShell
    :open="open"
    title="녹음 내용"
    title-id="recording-text-modal-title"
    @close="emit('close')"
  >
    <div class="recording-text-modal__body overflow-y-auto">
      <p v-if="hasText" class="recording-text-modal__text mb-0">{{ text }}</p>
      <p v-else class="recording-text-modal__empty mb-0 text-center">
        변환된 녹음 텍스트가 아직 없어요.
      </p>
    </div>
    <template #footer>
      <button
        type="button"
        class="recording-text-modal__close btn w-100 fw-semibold"
        data-bs-dismiss="modal"
      >닫기</button>
    </template>
  </RecordingModalShell>
</template>

<style scoped>
.recording-text-modal__body { max-height: min(50dvh, 360px); }
.recording-text-modal__text {
  color: var(--black-500);
  font-size: 0.875rem;
  line-height: 1.75;
  white-space: pre-wrap;
}
.recording-text-modal__empty {
  padding: 24px 0;
  color: var(--black-300);
  font-size: 0.8125rem;
}
.recording-text-modal__close {
  height: 44px;
  color: white;
  background-color: var(--blue-900);
  border: 0;
  border-radius: 12px;
}
.recording-text-modal__close:hover,
.recording-text-modal__close:focus {
  color: white;
  background-color: var(--blue-700);
}
</style>
