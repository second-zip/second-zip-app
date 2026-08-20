import { beforeEach, describe, expect, test, vi } from 'vitest';

import api from './instance';
import {
  deleteRecording,
  getRecordingFileUrl,
  getRecordingStatus,
  getRecordingTranscript,
  startLiveRecording,
  stopLiveRecording,
} from './recording';

vi.mock('./instance', () => ({
  default: { delete: vi.fn(), get: vi.fn(), post: vi.fn() },
}));

describe('recording API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.get.mockResolvedValue({ data: { status: 'COMPLETED' } });
    api.post.mockResolvedValue({ data: { recordingSessionId: 3 } });
    api.delete.mockResolvedValue({});
  });

  test('실시간 녹음 세션을 생성하고 응답 데이터를 반환한다', async () => {
    await expect(startLiveRecording(12)).resolves.toEqual({
      recordingSessionId: 3,
    });
    expect(api.post).toHaveBeenCalledWith('/recordings/live', {
      reportChecklistId: 12,
    });
  });

  test.each([
    [getRecordingStatus, '/recordings/3'],
    [getRecordingFileUrl, '/recordings/3/file-url'],
    [getRecordingTranscript, '/recordings/3/transcript'],
  ])('녹음 조회 API의 data를 반환한다', async (request, url) => {
    await expect(request(3)).resolves.toEqual({ status: 'COMPLETED' });
    expect(api.get).toHaveBeenCalledWith(url);
  });

  test('녹음 종료 시 WAV 파일을 multipart/form-data로 전달한다', async () => {
    const recordingFile = new Blob(['wav'], { type: 'audio/wav' });

    await stopLiveRecording(3, recordingFile);

    expect(api.post).toHaveBeenCalledWith(
      '/recordings/3/stop',
      expect.any(FormData),
      { timeout: 120_000 },
    );
    const formData = api.post.mock.calls[0][1];
    expect(formData.get('file')).toEqual(expect.objectContaining({
      name: 'recording-3.wav', type: 'audio/wav', size: 3,
    }));
  });

  test('파일 없는 녹음 종료와 삭제 API를 호출한다', async () => {
    await stopLiveRecording(3);
    await deleteRecording(3);

    expect(api.post).toHaveBeenCalledWith('/recordings/3/stop');
    expect(api.delete).toHaveBeenCalledWith('/recordings/3');
  });
});
