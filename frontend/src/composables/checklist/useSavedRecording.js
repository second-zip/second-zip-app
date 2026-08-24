import {
  computed, onBeforeUnmount, ref, toValue, watch,
} from 'vue';

import {
  deleteRecording,
  getRecordingFileUrl,
  getRecordingTranscript,
} from '@/api/recording';
import { getApiError } from '@/api/utils/error';

const URL_REFRESH_BUFFER_MS = 30_000;

export const useSavedRecording = (emit, initialRecordingSessionId) => {
  const savedRecording = ref(null);
  const isLoadingRecording = ref(false);
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
  let loadSequence = 0;

  const revokeLocalUrl = (recording) => {
    if (recording?.url?.startsWith('blob:')) {
      URL.revokeObjectURL(recording.url);
    }
  };
  const makeRemoteRecording = (recordingSessionId, fileData) => ({
    ...fileData,
    recordingSessionId,
    transcript: '',
    duration: 0,
    urlKind: 'remote',
    expiresAt: Date.now() + (Number(fileData.expiresIn) || 0) * 1000,
  });

  const loadRemote = async (recordingSessionId) => {
    if (!recordingSessionId) return;
    const sequence = ++loadSequence;
    isLoadingRecording.value = true;
    errorMessage.value = '';
    if (savedRecording.value?.recordingSessionId !== recordingSessionId) {
      revokeLocalUrl(savedRecording.value);
      savedRecording.value = null;
    }
    try {
      const fileData = await getRecordingFileUrl(recordingSessionId);
      const expectedRecordingSessionId = toValue(initialRecordingSessionId);
      if (
        sequence !== loadSequence
        || (
          expectedRecordingSessionId != null
          && expectedRecordingSessionId !== recordingSessionId
        )
        || savedRecording.value?.recordingSessionId === recordingSessionId
      ) return;
      revokeLocalUrl(savedRecording.value);
      savedRecording.value = makeRemoteRecording(recordingSessionId, fileData);
    } catch (error) {
      if (sequence !== loadSequence) return;
      if (error.response?.status === 404) {
        if (savedRecording.value?.urlKind === 'remote') {
          savedRecording.value = null;
        }
        return;
      }
      errorMessage.value = getApiError(error).message;
    } finally {
      if (sequence === loadSequence) isLoadingRecording.value = false;
    }
  };

  const save = (localRecording, statusData) => {
    const recordingSessionId = statusData.recordingSessionId;
    loadSequence += 1;
    isLoadingRecording.value = false;
    errorMessage.value = '';
    revokeLocalUrl(savedRecording.value);
    savedRecording.value = {
      ...localRecording,
      recordingSessionId,
      transcript: statusData.transcript ?? '',
      url: URL.createObjectURL(localRecording.blob),
      urlKind: 'blob',
    };
  };

  const ensureFreshUrl = async () => {
    const recording = savedRecording.value;
    if (
      !recording
      || recording.urlKind !== 'remote'
      || recording.expiresAt > Date.now() + URL_REFRESH_BUFFER_MS
    ) return;
    try {
      const fileData = await getRecordingFileUrl(recording.recordingSessionId);
      if (savedRecording.value !== recording) return;
      Object.assign(
        recording,
        fileData,
        { expiresAt: Date.now() + (Number(fileData.expiresIn) || 0) * 1000 },
      );
      errorMessage.value = '';
    } catch (error) {
      errorMessage.value = getApiError(error).message;
      throw error;
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
      loadSequence += 1;
      revokeLocalUrl(savedRecording.value);
      savedRecording.value = null;
      errorMessage.value = '';
      isDeleteModalOpen.value = false;
      emit('processed');
    } catch (error) {
      deleteErrorMessage.value = getApiError(error).message;
    } finally {
      isDeleting.value = false;
    }
  };

  watch(
    () => toValue(initialRecordingSessionId),
    (recordingSessionId) => {
      if (!recordingSessionId) {
        loadSequence += 1;
        isLoadingRecording.value = false;
        if (savedRecording.value?.urlKind === 'remote') {
          savedRecording.value = null;
        }
        return;
      }
      if (savedRecording.value?.recordingSessionId !== recordingSessionId) {
        void loadRemote(recordingSessionId);
      }
    },
    { immediate: true },
  );
  watch(hasOpenModal, (open) => emit('modal-visibility-change', open));
  onBeforeUnmount(() => {
    loadSequence += 1;
    revokeLocalUrl(savedRecording.value);
  });
  return {
    deleteErrorMessage, ensureFreshUrl, errorMessage, isDeleteModalOpen,
    isDeleting, isLoadingRecording, isTextLoading, isTextModalOpen,
    loadRemote, openTextModal, remove, save, savedRecording, textErrorMessage,
  };
};
