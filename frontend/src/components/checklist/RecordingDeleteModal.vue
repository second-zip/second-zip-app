<script setup>
import RecordingModalShell from './RecordingModalShell.vue';

defineProps({
  open: { type: Boolean, default: false },
  isDeleting: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
});
const emit = defineEmits(['close', 'confirm']);
</script>

<template>
  <RecordingModalShell
    :open="open"
    title="녹음을 삭제할까요?"
    title-id="recording-delete-modal-title"
    @close="emit('close')"
  >
    <div class="recording-delete-modal__body">
      <p class="mb-1">삭제한 녹음 기록은 복구할 수 없어요.</p>
      <p class="mb-0">
        녹음을 삭제해도 현재 체크리스트의 체크 상태는 변경되지 않아요.
      </p>
      <p v-if="errorMessage" class="recording-delete-modal__error mt-2 mb-0" role="alert">
        {{ errorMessage }}
      </p>
    </div>
    <template #footer>
      <button
        type="button"
        class="btn btn-light"
        data-bs-dismiss="modal"
        :disabled="isDeleting"
      >
        취소
      </button>
      <button
        type="button"
        class="btn btn-danger"
        :disabled="isDeleting"
        @click="emit('confirm')"
      >{{ isDeleting ? '삭제 중' : '녹음 삭제' }}</button>
    </template>
  </RecordingModalShell>
</template>

<style scoped>
.recording-delete-modal__body {
  color: var(--black-500);
  font-size: 0.8125rem;
  line-height: 1.6;
}
.recording-delete-modal__error { color: var(--red-500); }
</style>
