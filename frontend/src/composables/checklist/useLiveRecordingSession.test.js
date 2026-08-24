import { flushPromises, mount } from '@vue/test-utils';
import { ref } from 'vue';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { useLiveRecordingSession } from './useLiveRecordingSession';

const mocks = vi.hoisted(() => ({
  connect: vi.fn(), close: vi.fn(), send: vi.fn(), waitUntilSent: vi.fn(),
  getStatus: vi.fn(), start: vi.fn(), stop: vi.fn(),
  socketErrorHandler: null,
}));

vi.mock('@/api/recording', () => ({
  getRecordingStatus: mocks.getStatus,
  startLiveRecording: mocks.start,
  stopLiveRecording: mocks.stop,
}));
vi.mock('@/services/recordingSocket', () => ({
  createRecordingSocket: (onError) => {
    mocks.socketErrorHandler = onError;
    return {
      close: mocks.close, connect: mocks.connect, send: mocks.send,
      waitUntilSent: mocks.waitUntilSent,
    };
  },
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
    mocks.socketErrorHandler = null;
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

  test('WebSocket의 비동기 오류를 사용자에게 표시한다', () => {
    const { state } = setup();

    mocks.socketErrorHandler(new Error('소켓 전송 오류'));

    expect(state.errorMessage.value).toBe('소켓 전송 오류');
  });

  test('분석 상태 조회 실패를 표시하고 중단 시 타이머와 세션을 정리한다', async () => {
    mocks.getStatus.mockRejectedValue({
      response: { data: { message: '분석 상태를 확인할 수 없어요.' } },
    });
    const { state } = setup();
    await state.start();

    await state.finish(new Blob(['wav']));
    await flushPromises();

    expect(state.errorMessage.value).toBe('분석 상태를 확인할 수 없어요.');
    expect(state.isProcessing.value).toBe(true);

    await state.abort();
    expect(state.recordingSessionId.value).toBeNull();
    expect(state.isProcessing.value).toBe(false);
  });

  test('녹음 종료 요청 실패 시 연결을 닫고 오류를 다시 전달한다', async () => {
    mocks.waitUntilSent.mockRejectedValue({
      response: { data: { message: '녹음을 종료할 수 없어요.' } },
    });
    const { state } = setup();
    await state.start();

    await expect(state.finish(new Blob(['wav']))).rejects.toBeTruthy();

    expect(mocks.close).toHaveBeenCalled();
    expect(state.isProcessing.value).toBe(false);
    expect(state.errorMessage.value).toBe('녹음을 종료할 수 없어요.');
  });

  test('녹음 중 중단하면 서버 세션도 종료한다', async () => {
    const { state } = setup();
    await state.start();

    await state.abort();

    expect(mocks.stop).toHaveBeenCalledWith(4);
    expect(state.status.value).toBeNull();
  });

  test('컴포넌트 해제 시 진행 중인 녹음 리소스를 정리한다', async () => {
    const { state, wrapper } = setup();
    await state.start();

    wrapper.unmount();
    await flushPromises();

    expect(mocks.close).toHaveBeenCalled();
    expect(mocks.stop).toHaveBeenCalledWith(4);
  });
});
