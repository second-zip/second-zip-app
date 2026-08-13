import { beforeEach, describe, expect, test, vi } from 'vitest';

import api from './instance';
import {
  deleteRecording,
  getRecording,
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
    [getRecording, '/recordings/3/read'],
    [getRecordingTranscript, '/recordings/3/transcript'],
  ])('녹음 조회 API의 data를 반환한다', async (request, url) => {
    await expect(request(3)).resolves.toEqual({ status: 'COMPLETED' });
    expect(api.get).toHaveBeenCalledWith(url);
  });

  test('녹음 종료와 삭제 API를 호출한다', async () => {
    await stopLiveRecording(3);
    await deleteRecording(3);

    expect(api.post).toHaveBeenCalledWith('/recordings/3/stop');
    expect(api.delete).toHaveBeenCalledWith('/recordings/3');
  });
});
