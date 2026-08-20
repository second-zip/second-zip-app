import api from './instance';

export const startLiveRecording = async (reportChecklistId) => {
  const response = await api.post('/recordings/live', { reportChecklistId });
  return response.data;
};

export const stopLiveRecording = async (recordingSessionId, recordingFile) => {
  const url = `/recordings/${recordingSessionId}/stop`;
  if (!recordingFile) {
    await api.post(url);
    return;
  }

  const formData = new FormData();
  formData.append('file', recordingFile, `recording-${recordingSessionId}.wav`);
  await api.post(url, formData, { timeout: 120_000 });
};

export const getRecordingStatus = async (recordingSessionId) => {
  const response = await api.get(`/recordings/${recordingSessionId}`);
  return response.data;
};

export const getRecording = async (recordingSessionId) => {
  const response = await api.get(`/recordings/${recordingSessionId}/read`);
  return response.data;
};

export const getRecordingTranscript = async (recordingSessionId) => {
  const response = await api.get(
    `/recordings/${recordingSessionId}/transcript`,
  );
  return response.data;
};

export const deleteRecording = async (recordingSessionId) => {
  await api.delete(`/recordings/${recordingSessionId}`);
};
