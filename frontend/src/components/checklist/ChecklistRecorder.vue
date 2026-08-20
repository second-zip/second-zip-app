<script setup>
import { toRef } from 'vue';

import { useChecklistRecorder } from '@/composables/checklist/useChecklistRecorder';
import RecordingActivePanel from './RecordingActivePanel.vue';
import RecordingDeleteModal from './RecordingDeleteModal.vue';
import RecordingIdlePanel from './RecordingIdlePanel.vue';
import RecordingProcessingPanel from './RecordingProcessingPanel.vue';
import RecordingSavedPanel from './RecordingSavedPanel.vue';
import RecordingTextModal from './RecordingTextModal.vue';

const props = defineProps({
  reportChecklistId: { type: Number, required: true },
  recordingSessionId: { type: Number, default: null },
});
const emit = defineEmits(['modal-visibility-change', 'processed']);
const state = useChecklistRecorder(
  emit,
  toRef(props, 'reportChecklistId'),
  toRef(props, 'recordingSessionId'),
);
</script>

<template>
  <section class="checklist-recorder w-100" aria-label="체크리스트 녹음">
    <div
      class="checklist-recorder__panel"
      :aria-live="state.isRecording.value ? 'polite' : undefined"
    >
      <RecordingActivePanel
        v-if="state.isRecording.value || state.isFinishing.value"
        :elapsed-seconds="state.elapsedSeconds.value"
        :levels="state.waveformLevels.value"
        :finishing="state.isFinishing.value"
        @finish="state.finishRecording"
      />
      <RecordingProcessingPanel v-else-if="state.isProcessing.value" />
      <RecordingSavedPanel
        v-else-if="state.savedRecording.value"
        :recording="state.savedRecording.value"
        :before-play="state.ensureFreshUrl"
        can-delete
        @delete="state.isDeleteModalOpen.value = true"
        @show-text="state.openTextModal"
      />
      <RecordingIdlePanel
        v-else
        :starting="state.isStarting.value || state.isLoadingRecording.value"
        @start="state.beginRecording"
      />
    </div>

    <p v-if="state.errorMessage.value" class="checklist-recorder__error mt-2 mb-0" role="alert">
      {{ state.errorMessage.value }}
    </p>
    <RecordingDeleteModal
      :open="state.isDeleteModalOpen.value"
      :is-deleting="state.isDeleting.value"
      :error-message="state.deleteErrorMessage.value"
      @close="state.isDeleteModalOpen.value = false"
      @confirm="state.remove"
    />
    <RecordingTextModal
      :open="state.isTextModalOpen.value"
      :text="state.savedRecording.value?.transcript ?? ''"
      :is-loading="state.isTextLoading.value"
      :error-message="state.textErrorMessage.value"
      @close="state.isTextModalOpen.value = false"
    />
  </section>
</template>

<style scoped>
.checklist-recorder__panel {
  min-height: 52px;
  padding: 10px 12px;
  background-color: white;
  border: 1px solid var(--black-100);
  border-radius: 16px;
  box-shadow: 0 2px 8px rgb(17 17 24 / 5%);
}

.checklist-recorder__error {
  color: var(--red-500);
  font-size: 0.6875rem;
}
</style>
