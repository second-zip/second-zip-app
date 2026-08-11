<script setup>
import { useChecklistRecorder } from '@/composables/checklist/useChecklistRecorder';
import RecordingActivePanel from './RecordingActivePanel.vue';
import RecordingDeleteModal from './RecordingDeleteModal.vue';
import RecordingIdlePanel from './RecordingIdlePanel.vue';
import RecordingSavedPanel from './RecordingSavedPanel.vue';
import RecordingTextModal from './RecordingTextModal.vue';

const emit = defineEmits([
  'modal-visibility-change', 'recording-chunk', 'recording-complete',
]);
const state = useChecklistRecorder(emit);
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
      <RecordingSavedPanel
        v-else-if="state.savedRecording.value"
        :recording="state.savedRecording.value"
        @delete="state.isDeleteModalOpen.value = true"
        @show-text="state.openTextModal"
      />
      <RecordingIdlePanel
        v-else
        :starting="state.isStarting.value"
        @start="state.beginRecording"
      />
    </div>

    <p v-if="state.errorMessage.value" class="checklist-recorder__error mt-2 mb-0" role="alert">
      {{ state.errorMessage.value }}
    </p>
    <RecordingDeleteModal
      :open="state.isDeleteModalOpen.value"
      @close="state.isDeleteModalOpen.value = false"
      @confirm="state.deleteRecording"
    />
    <RecordingTextModal
      :open="state.isTextModalOpen.value"
      :text="state.savedRecording.value?.transcript ?? ''"
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
