import { computed, onBeforeUnmount, ref, watch } from 'vue';

import { useAudioRecorder } from './useAudioRecorder';

export const useChecklistRecorder = (emit) => {
  const recorder = useAudioRecorder();
  const savedRecording = ref(null);
  const isDeleteModalOpen = ref(false);
  const isFinishing = ref(false);
  const isTextModalOpen = ref(false);
  const hasOpenModal = computed(
    () => isDeleteModalOpen.value || isTextModalOpen.value,
  );

  const beginRecording = async () => {
    recorder.clearError();
    try {
      await recorder.startRecording({
        onChunk: (chunk) => emit('recording-chunk', chunk),
      });
    } catch {
      // 권한 및 장치 오류는 녹음 영역에서 안내합니다.
    }
  };

  const finishRecording = async () => {
    if (isFinishing.value) return;
    isFinishing.value = true;
    const duration = recorder.elapsedSeconds.value;
    const blob = await recorder.stopRecording();

    if (blob) {
      if (savedRecording.value?.url) {
        URL.revokeObjectURL(savedRecording.value.url);
      }
      savedRecording.value = {
        blob, duration, transcript: '', url: URL.createObjectURL(blob),
      };
      emit('recording-complete', blob);
    }
    isFinishing.value = false;
  };

  const deleteRecording = () => {
    // 실제 삭제 API 성공 후 아래 로컬 상태를 비우도록 연결합니다.
    if (savedRecording.value?.url) {
      URL.revokeObjectURL(savedRecording.value.url);
    }
    savedRecording.value = null;
  };

  const openTextModal = () => {
    // STT 조회 API 결과로 transcript를 갱신한 뒤 모달을 엽니다.
    isTextModalOpen.value = true;
  };

  watch(hasOpenModal, (open) => emit('modal-visibility-change', open));
  onBeforeUnmount(() => {
    if (savedRecording.value?.url) {
      URL.revokeObjectURL(savedRecording.value.url);
    }
  });

  return {
    ...recorder, beginRecording, deleteRecording, finishRecording,
    isDeleteModalOpen, isFinishing, isTextModalOpen, openTextModal,
    savedRecording,
  };
};
