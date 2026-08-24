import { onBeforeUnmount, ref, toValue } from 'vue';

import {
  getRecordingStatus,
  startLiveRecording,
  stopLiveRecording,
} from '@/api/recording';
import { getApiError } from '@/api/utils/error';
import { createRecordingSocket } from '@/services/recordingSocket';

const POLL_INTERVAL = 1500;

export const useLiveRecordingSession = (reportChecklistId, onComplete) => {
  const recordingSessionId = ref(null);
  const status = ref(null);
  const transcript = ref('');
  const errorMessage = ref('');
  const isProcessing = ref(false);
  const socket = createRecordingSocket((error) => {
    errorMessage.value = error.message;
  });
  let pollTimer;
  let disposed = false;

  const applyStatus = (data) => {
    status.value = data.status;
    transcript.value = data.transcript ?? '';
    errorMessage.value = '';
    if (data.status === 'FAILED') {
      isProcessing.value = false;
      errorMessage.value = data.failureReason || '녹음 분석에 실패했어요.';
    }
    if (data.status === 'COMPLETED') {
      isProcessing.value = false;
      onComplete?.(data);
    }
    return data;
  };

  const refresh = async () => applyStatus(
    await getRecordingStatus(recordingSessionId.value),
  );
  const poll = async () => {
    if (disposed || !isProcessing.value) return;
    try {
      await refresh();
    } catch (error) {
      errorMessage.value = getApiError(error).message;
    }
    if (!disposed && isProcessing.value) {
      pollTimer = window.setTimeout(poll, POLL_INTERVAL);
    }
  };

  const start = async () => {
    errorMessage.value = '';
    try {
      const data = await startLiveRecording(toValue(reportChecklistId));
      recordingSessionId.value = data.recordingSessionId;
      status.value = data.status;
      await socket.connect(data.recordingSessionId);
    } catch (error) {
      errorMessage.value = error.message?.includes('녹음 서버')
        ? error.message
        : getApiError(error).message;
      socket.close();
      const failedSessionId = recordingSessionId.value;
      if (failedSessionId && status.value === 'RECORDING') {
        void stopLiveRecording(failedSessionId).catch(() => {});
      }
      recordingSessionId.value = null;
      status.value = null;
      throw error;
    }
  };

  const finish = async (recordingFile) => {
    try {
      await socket.waitUntilSent();
      await stopLiveRecording(recordingSessionId.value, recordingFile);
      socket.close();
      status.value = 'ANALYZING';
      isProcessing.value = true;
      void poll();
    } catch (error) {
      socket.close();
      isProcessing.value = false;
      errorMessage.value = getApiError(error).message;
      throw error;
    }
  };

  const abort = async () => {
    window.clearTimeout(pollTimer);
    socket.close();
    const sessionId = recordingSessionId.value;
    const shouldStop = status.value === 'RECORDING' && sessionId;
    recordingSessionId.value = null;
    status.value = null;
    isProcessing.value = false;
    if (shouldStop) await stopLiveRecording(sessionId).catch(() => {});
  };

  onBeforeUnmount(() => {
    disposed = true;
    void abort();
  });

  return {
    abort, errorMessage, finish, isProcessing, recordingSessionId,
    refresh, sendChunk: socket.send, start, status, transcript,
  };
};
