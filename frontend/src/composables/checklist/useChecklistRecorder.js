import { computed, ref } from 'vue';

import { useAudioRecorder } from './useAudioRecorder';
import { useLiveRecordingSession } from './useLiveRecordingSession';
import { useSavedRecording } from './useSavedRecording';

export const useChecklistRecorder = (emit, reportChecklistId) => {
  const recorder = useAudioRecorder();
  const isFinishing = ref(false);
  const saved = useSavedRecording(emit);
  let pendingRecording;

  const setSavedRecording = (data) => {
    if (!pendingRecording) return;
    void saved.save(pendingRecording, data);
    pendingRecording = undefined;
    emit('processed');
  };
  const live = useLiveRecordingSession(reportChecklistId, setSavedRecording);
  const errorMessage = computed(
    () => live.errorMessage.value
      || recorder.errorMessage.value
      || saved.errorMessage.value,
  );

  const beginRecording = async () => {
    recorder.clearError();
    live.errorMessage.value = '';
    try {
      await recorder.startRecording({
        beforeStart: live.start,
        onPcmChunk: live.sendChunk,
      });
    } catch {
      void live.abort();
      // Composables expose a user-safe error message in the recording panel.
    }
  };

  const finishRecording = async () => {
    if (isFinishing.value) return;
    isFinishing.value = true;
    const duration = recorder.elapsedSeconds.value;
    const blob = await recorder.stopRecording();
    if (blob) pendingRecording = { blob, duration };
    try {
      await live.finish(blob);
    } catch {
      pendingRecording = undefined;
    } finally {
      isFinishing.value = false;
    }
  };

  return {
    ...recorder, ...live, ...saved, beginRecording, errorMessage,
    finishRecording, isFinishing,
  };
};
