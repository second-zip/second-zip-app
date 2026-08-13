import api from './instance';

export const startLiveRecording = async (reportChecklistId) => {
  const response = await api.post('/recordings/live', { reportChecklistId });
  return response.data;
};

export const stopLiveRecording = async (recordingSessionId) => {
  await api.post(`/recordings/${recordingSessionId}/stop`);
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
