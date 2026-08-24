import { onBeforeUnmount, ref } from 'vue';

import { createPcmWavBlob } from '@/utils/wav';

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
  let pcmChunks = [];
  let elapsedTimer;
  let mediaStream;

  const releaseInput = () => {
    window.clearInterval(elapsedTimer);
    elapsedTimer = undefined;
    stopAnalysis();
    mediaStream?.getTracks().forEach((track) => track.stop());
    mediaStream = undefined;
  };

  const startRecording = async ({ beforeStart, onPcmChunk } = {}) => {
    if (isRecording.value || isStarting.value) return;
    errorMessage.value = '';
    isStarting.value = true;
    try {
      if (!navigator.mediaDevices?.getUserMedia) {
        throw new Error('Audio recording is not supported.');
      }
      mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: { ideal: 1 },
          sampleRate: { ideal: 16000 },
        },
      });
      startAnalysis(mediaStream);
      await beforeStart?.();
      pcmChunks = [];
      await startPcmStream(mediaStream, (chunk) => {
        pcmChunks.push(chunk);
        onPcmChunk?.(chunk);
      });
      elapsedSeconds.value = 0;
      const startedAt = Date.now();
      isRecording.value = true;
      elapsedTimer = window.setInterval(() => {
        elapsedSeconds.value = Math.floor((Date.now() - startedAt) / 1000);
      }, 250);
    } catch (error) {
      errorMessage.value = getErrorMessage(error);
      isRecording.value = false;
      pcmChunks = [];
      await stopPcmStream();
      releaseInput();
      throw error;
    } finally {
      isStarting.value = false;
    }
  };
  const stopRecording = async () => {
    if (!isRecording.value) return null;
    isRecording.value = false;
    try {
      await stopPcmStream();
      return createPcmWavBlob(pcmChunks);
    } catch {
      errorMessage.value = '녹음 데이터를 저장하지 못했어요.';
      return null;
    } finally {
      pcmChunks = [];
      releaseInput();
    }
  };

  onBeforeUnmount(() => {
    isRecording.value = false;
    pcmChunks = [];
    releaseInput();
    void stopPcmStream();
  });
  return {
    clearError: () => (errorMessage.value = ''), elapsedSeconds, errorMessage,
    isRecording, isStarting, startRecording, stopRecording, waveformLevels,
  };
};
