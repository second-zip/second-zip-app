import { onBeforeUnmount, ref } from 'vue';

import { useAudioAnalyser } from './useAudioAnalyser';

const getErrorMessage = (error) => {
  if (error?.name === 'NotAllowedError') return '마이크 권한을 허용해 주세요.';
  if (error?.name === 'NotFoundError') {
    return '사용 가능한 마이크를 찾을 수 없어요.';
  }
  return '녹음을 시작할 수 없어요. 잠시 후 다시 시도해 주세요.';
};

export const useAudioRecorder = () => {
  const isRecording = ref(false);
  const isStarting = ref(false);
  const elapsedSeconds = ref(0);
  const errorMessage = ref('');
  const { startAnalysis, stopAnalysis, waveformLevels } = useAudioAnalyser();
  let chunks = [];
  let elapsedTimer;
  let mediaRecorder;
  let mediaStream;
  let startedAt = 0;
  let stopResolver;

  const releaseInput = () => {
    window.clearInterval(elapsedTimer);
    elapsedTimer = undefined;
    stopAnalysis();
    mediaStream?.getTracks().forEach((track) => track.stop());
    mediaStream = undefined;
  };

  const attachRecorderEvents = (recorder, onChunk) => {
    recorder.addEventListener('dataavailable', ({ data }) => {
      if (!data.size) return;
      chunks.push(data);
      onChunk?.(data);
    });
    recorder.addEventListener('stop', () => {
      const blob = new Blob(chunks, {
        type: recorder.mimeType || 'audio/webm',
      });
      chunks = [];
      stopResolver?.(blob);
      stopResolver = undefined;
      if (mediaRecorder === recorder) mediaRecorder = undefined;
    });
    recorder.addEventListener('error', () => {
      errorMessage.value = '녹음 데이터를 저장하지 못했어요.';
      isRecording.value = false;
      releaseInput();
      stopResolver?.(null);
      stopResolver = undefined;
    });
  };

  const startRecording = async ({ onChunk } = {}) => {
    if (isRecording.value || isStarting.value) return;
    errorMessage.value = '';
    isStarting.value = true;

    try {
      if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
        throw new Error('Audio recording is not supported.');
      }
      mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
      chunks = [];
      mediaRecorder = new MediaRecorder(mediaStream);
      attachRecorderEvents(mediaRecorder, onChunk);
      elapsedSeconds.value = 0;
      startedAt = Date.now();
      isRecording.value = true;
      startAnalysis(mediaStream);
      mediaRecorder.start(1000);
      elapsedTimer = window.setInterval(() => {
        elapsedSeconds.value = Math.floor((Date.now() - startedAt) / 1000);
      }, 250);
    } catch (error) {
      errorMessage.value = getErrorMessage(error);
      isRecording.value = false;
      mediaRecorder = undefined;
      releaseInput();
      throw error;
    } finally {
      isStarting.value = false;
    }
  };

  const stopRecording = () => {
    if (!mediaRecorder || mediaRecorder.state === 'inactive') {
      return Promise.resolve(null);
    }
    return new Promise((resolve) => {
      stopResolver = resolve;
      isRecording.value = false;
      mediaRecorder.stop();
      releaseInput();
    });
  };

  onBeforeUnmount(() => {
    if (mediaRecorder?.state !== 'inactive') mediaRecorder.stop();
    isRecording.value = false;
    releaseInput();
    stopResolver = undefined;
  });

  return {
    clearError: () => (errorMessage.value = ''), elapsedSeconds, errorMessage,
    isRecording, isStarting, startRecording, stopRecording, waveformLevels,
  };
};
