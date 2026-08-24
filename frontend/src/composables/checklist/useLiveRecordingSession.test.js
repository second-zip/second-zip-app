import { flushPromises, mount } from '@vue/test-utils';
import { ref } from 'vue';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { useLiveRecordingSession } from './useLiveRecordingSession';

const mocks = vi.hoisted(() => ({
  connect: vi.fn(), close: vi.fn(), send: vi.fn(), waitUntilSent: vi.fn(),
  getStatus: vi.fn(), start: vi.fn(), stop: vi.fn(),
}));

vi.mock('@/api/recording', () => ({
  getRecordingStatus: mocks.getStatus,
  startLiveRecording: mocks.start,
  stopLiveRecording: mocks.stop,
}));
vi.mock('@/services/recordingSocket', () => ({
  createRecordingSocket: () => ({
    close: mocks.close, connect: mocks.connect, send: mocks.send,
    waitUntilSent: mocks.waitUntilSent,
  }),
}));

const setup = (onComplete = vi.fn()) => {
  let state;
  const wrapper = mount({
    setup() {
      state = useLiveRecordingSession(ref(9), onComplete);
      return () => null;
    },
  });
  return { onComplete, state, wrapper };
};

describe('useLiveRecordingSession', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.start.mockResolvedValue({ recordingSessionId: 4, status: 'RECORDING' });
    mocks.connect.mockResolvedValue(undefined);
    mocks.waitUntilSent.mockResolvedValue(undefined);
    mocks.stop.mockResolvedValue(undefined);
    mocks.getStatus.mockResolvedValue({
      recordingSessionId: 4, status: 'COMPLETED', transcript: '완료',
    });
  });

  test('체크리스트 ID로 세션과 WebSocket 연결을 시작한다', async () => {
    const { state } = setup();

    await state.start();

    expect(mocks.start).toHaveBeenCalledWith(9);
    expect(mocks.connect).toHaveBeenCalledWith(4);
    expect(state.recordingSessionId.value).toBe(4);
    expect(state.status.value).toBe('RECORDING');
  });

  test('종료 후 분석 완료 상태를 조회하고 완료 콜백을 호출한다', async () => {
    const { onComplete, state } = setup();
    const recordingFile = new Blob(['wav'], { type: 'audio/wav' });
    await state.start();

    await state.finish(recordingFile);
    await flushPromises();

    expect(mocks.waitUntilSent).toHaveBeenCalledOnce();
    expect(mocks.stop).toHaveBeenCalledWith(4, recordingFile);
    expect(mocks.getStatus).toHaveBeenCalledWith(4);
    expect(state.isProcessing.value).toBe(false);
    expect(onComplete).toHaveBeenCalledWith(expect.objectContaining({
      status: 'COMPLETED', transcript: '완료',
    }));
  });

  test('WebSocket 연결 실패 시 생성한 녹음 세션을 정리한다', async () => {
    mocks.connect.mockRejectedValue(new Error('녹음 서버에 연결하지 못했어요.'));
    const { state } = setup();

    await expect(state.start()).rejects.toThrow('녹음 서버');
    await flushPromises();

    expect(mocks.stop).toHaveBeenCalledWith(4);
    expect(mocks.close).toHaveBeenCalled();
    expect(state.recordingSessionId.value).toBeNull();
    expect(state.errorMessage.value).toContain('녹음 서버');
  });

  test('분석 실패 응답의 사유를 사용자에게 표시한다', async () => {
    mocks.getStatus.mockResolvedValue({ status: 'FAILED', failureReason: '음성 없음' });
    const { state } = setup();
    const recordingFile = new Blob(['wav'], { type: 'audio/wav' });
    await state.start();

    await state.finish(recordingFile);
    await flushPromises();

    expect(state.isProcessing.value).toBe(false);
    expect(state.errorMessage.value).toBe('음성 없음');
  });
});
