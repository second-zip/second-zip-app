import { computed, onBeforeUnmount, ref, watch } from 'vue';

import {
  deleteRecording,
  getRecording,
  getRecordingTranscript,
} from '@/api/recording';
import { getApiError } from '@/api/utils/error';

export const useSavedRecording = (emit) => {
  const savedRecording = ref(null);
  const isDeleteModalOpen = ref(false);
  const isDeleting = ref(false);
  const deleteErrorMessage = ref('');
  const isTextModalOpen = ref(false);
  const isTextLoading = ref(false);
  const textErrorMessage = ref('');
  const errorMessage = ref('');
  const hasOpenModal = computed(
    () => isDeleteModalOpen.value || isTextModalOpen.value,
  );

  const save = async (localRecording, statusData) => {
    const recordingSessionId = statusData.recordingSessionId;
    errorMessage.value = '';
    savedRecording.value = {
      ...localRecording,
      recordingSessionId,
      transcript: statusData.transcript ?? '',
      url: URL.createObjectURL(localRecording.blob),
    };
    const targetRecording = savedRecording.value;
    try {
      const detail = await getRecording(recordingSessionId);
      if (savedRecording.value === targetRecording) {
        Object.assign(targetRecording, detail);
      }
    } catch (error) {
      errorMessage.value = getApiError(error).message;
    }
  };

  const openTextModal = async () => {
    if (isTextLoading.value || !savedRecording.value) return;
    isTextModalOpen.value = true;
    isTextLoading.value = true;
    textErrorMessage.value = '';
    try {
      const data = await getRecordingTranscript(
        savedRecording.value.recordingSessionId,
      );
      savedRecording.value.transcript = data.transcript ?? '';
    } catch {
      textErrorMessage.value = '녹음 내용을 불러오지 못했어요.';
    } finally {
      isTextLoading.value = false;
    }
  };

  const remove = async () => {
    if (isDeleting.value || !savedRecording.value) return;
    isDeleting.value = true;
    deleteErrorMessage.value = '';
    try {
      await deleteRecording(savedRecording.value.recordingSessionId);
      URL.revokeObjectURL(savedRecording.value.url);
      savedRecording.value = null;
      errorMessage.value = '';
      isDeleteModalOpen.value = false;
    } catch (error) {
      deleteErrorMessage.value = getApiError(error).message;
    } finally {
      isDeleting.value = false;
    }
  };

  watch(hasOpenModal, (open) => emit('modal-visibility-change', open));
  onBeforeUnmount(() => {
    if (savedRecording.value?.url) URL.revokeObjectURL(savedRecording.value.url);
  });
  return {
    deleteErrorMessage, errorMessage, isDeleteModalOpen, isDeleting,
    isTextLoading, isTextModalOpen, openTextModal, remove, save,
    savedRecording, textErrorMessage,
  };
};
