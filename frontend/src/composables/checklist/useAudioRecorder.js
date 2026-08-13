import { onBeforeUnmount, ref } from 'vue';

import { useAudioAnalyser } from './useAudioAnalyser';
import { usePcmAudioStream } from './usePcmAudioStream';

const getErrorMessage = (error) => {
  if (error?.name === 'NotAllowedError') return '마이크 권한을 허용해 주세요.';
  if (error?.name === 'NotFoundError') return '사용 가능한 마이크를 찾을 수 없어요.';
  return '녹음을 시작할 수 없어요. 잠시 후 다시 시도해 주세요.';
};

export const useAudioRecorder = () => {
  const isRecording = ref(false);
  const isStarting = ref(false);
  const elapsedSeconds = ref(0);
  const errorMessage = ref('');
  const { startAnalysis, stopAnalysis, waveformLevels } = useAudioAnalyser();
  const { startPcmStream, stopPcmStream } = usePcmAudioStream();
  let chunks = [];
  let elapsedTimer;
  let mediaRecorder;
  let mediaStream;
  let stopResolver;

  const releaseInput = () => {
    window.clearInterval(elapsedTimer);
    elapsedTimer = undefined;
    stopAnalysis();
    mediaStream?.getTracks().forEach((track) => track.stop());
    mediaStream = undefined;
  };
  const attachEvents = (recorder) => {
    recorder.addEventListener('dataavailable', ({ data }) => {
      if (!data.size) return;
      chunks.push(data);
    });
    recorder.addEventListener('stop', () => {
      const options = recorder.mimeType ? { type: recorder.mimeType } : undefined;
      const blob = new Blob(chunks, options);
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

  const startRecording = async ({ beforeStart, onPcmChunk } = {}) => {
    if (isRecording.value || isStarting.value) return;
    errorMessage.value = '';
    isStarting.value = true;
    try {
      if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
        throw new Error('Audio recording is not supported.');
      }
      mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: { ideal: 1 },
          sampleRate: { ideal: 16000 },
        },
      });
      mediaRecorder = new MediaRecorder(mediaStream);
      attachEvents(mediaRecorder);
      startAnalysis(mediaStream);
      await beforeStart?.();
      await startPcmStream(mediaStream, onPcmChunk);
      chunks = [];
      elapsedSeconds.value = 0;
      const startedAt = Date.now();
      mediaRecorder.start(1000);
      isRecording.value = true;
      elapsedTimer = window.setInterval(() => {
        elapsedSeconds.value = Math.floor((Date.now() - startedAt) / 1000);
      }, 250);
    } catch (error) {
      errorMessage.value = getErrorMessage(error);
      isRecording.value = false;
      mediaRecorder = undefined;
      await stopPcmStream();
      releaseInput();
      throw error;
    } finally {
      isStarting.value = false;
    }
  };
  const stopRecording = async () => {
    if (!mediaRecorder || mediaRecorder.state === 'inactive') return Promise.resolve(null);
    await stopPcmStream();
    return new Promise((resolve) => {
      stopResolver = resolve;
      isRecording.value = false;
      mediaRecorder.stop();
      releaseInput();
    });
  };

  onBeforeUnmount(() => {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') mediaRecorder.stop();
    isRecording.value = false;
    releaseInput();
    void stopPcmStream();
    stopResolver = undefined;
  });
  return {
    clearError: () => (errorMessage.value = ''), elapsedSeconds, errorMessage,
    isRecording, isStarting, startRecording, stopRecording, waveformLevels,
  };
};
