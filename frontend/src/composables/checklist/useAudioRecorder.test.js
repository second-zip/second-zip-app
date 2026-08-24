import { mount } from '@vue/test-utils';
import {
  afterEach, beforeEach, describe, expect, test, vi,
} from 'vitest';

import { useAudioRecorder } from './useAudioRecorder';

const mocks = vi.hoisted(() => ({
  startAnalysis: vi.fn(), startPcm: vi.fn(),
  stopAnalysis: vi.fn(), stopPcm: vi.fn(),
}));

vi.mock('./useAudioAnalyser', () => ({
  useAudioAnalyser: () => ({
    startAnalysis: mocks.startAnalysis,
    stopAnalysis: mocks.stopAnalysis,
    waveformLevels: { value: Array(8).fill(0.08) },
  }),
}));
vi.mock('./usePcmAudioStream', () => ({
  usePcmAudioStream: () => ({
    startPcmStream: mocks.startPcm,
    stopPcmStream: mocks.stopPcm,
  }),
}));

const setup = () => {
  let recorder;
  const wrapper = mount({
    setup() {
      recorder = useAudioRecorder();
      return () => null;
    },
  });
  return { recorder, wrapper };
};

describe('useAudioRecorder', () => {
  let getUserMedia;
  let stream;
  let stopTrack;

  beforeEach(() => {
    vi.clearAllMocks();
    stopTrack = vi.fn();
    stream = { getTracks: () => [{ stop: stopTrack }] };
    getUserMedia = vi.fn().mockResolvedValue(stream);
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true, value: { getUserMedia },
    });
    mocks.startPcm.mockResolvedValue(undefined);
    mocks.stopPcm.mockResolvedValue(undefined);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  test('마이크·분석·PCM 전송을 시작하고 로컬 녹음 Blob을 만든다', async () => {
    const { recorder } = setup();
    const beforeStart = vi.fn();
    const onPcmChunk = vi.fn();
    const pcmChunk = new ArrayBuffer(4);
    new DataView(pcmChunk).setInt16(0, 1000, true);
    mocks.startPcm.mockImplementation(async (_, handleChunk) => {
      handleChunk(pcmChunk);
    });

    await recorder.startRecording({ beforeStart, onPcmChunk });
    const blob = await recorder.stopRecording();

    expect(getUserMedia).toHaveBeenCalledWith({ audio: {
      channelCount: { ideal: 1 }, sampleRate: { ideal: 16000 },
    } });
    expect(beforeStart).toHaveBeenCalledBefore(mocks.startPcm);
    expect(mocks.startPcm).toHaveBeenCalledWith(stream, expect.any(Function));
    expect(onPcmChunk).toHaveBeenCalledWith(pcmChunk);
    expect(blob.type).toBe('audio/wav');
    expect(blob.size).toBe(48);
  });

  test('마이크 권한 거부를 사용자 메시지로 변환하고 리소스를 정리한다', async () => {
    getUserMedia.mockRejectedValue(Object.assign(new Error(), {
      name: 'NotAllowedError',
    }));
    const { recorder } = setup();

    await expect(recorder.startRecording()).rejects.toThrow();

    expect(recorder.errorMessage.value).toBe('마이크 권한을 허용해 주세요.');
    expect(recorder.isRecording.value).toBe(false);
    expect(mocks.stopPcm).toHaveBeenCalled();
    expect(mocks.stopAnalysis).toHaveBeenCalled();
  });

  test('사용 가능한 마이크가 없을 때 원인에 맞는 메시지를 표시한다', async () => {
    getUserMedia.mockRejectedValue(Object.assign(new Error(), {
      name: 'NotFoundError',
    }));
    const { recorder } = setup();

    await expect(recorder.startRecording()).rejects.toThrow();

    expect(recorder.errorMessage.value).toBe('사용 가능한 마이크를 찾을 수 없어요.');
  });

  test('브라우저가 녹음을 지원하지 않을 때 일반 오류 메시지를 표시한다', async () => {
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true, value: undefined,
    });
    const { recorder } = setup();

    await expect(recorder.startRecording()).rejects.toThrow('not supported');

    expect(recorder.errorMessage.value).toContain('녹음을 시작할 수 없어요.');
  });

  test('녹음 중 경과 시간을 갱신한다', async () => {
    vi.useFakeTimers();
    const { recorder } = setup();

    await recorder.startRecording();
    await vi.advanceTimersByTimeAsync(1250);

    expect(recorder.elapsedSeconds.value).toBe(1);
    await recorder.stopRecording();
  });

  test('PCM 저장 실패 시 null을 반환하고 입력 리소스를 정리한다', async () => {
    const { recorder } = setup();
    await recorder.startRecording();
    mocks.stopPcm.mockRejectedValueOnce(new Error('save failed'));

    await expect(recorder.stopRecording()).resolves.toBeNull();

    expect(recorder.errorMessage.value).toBe('녹음 데이터를 저장하지 못했어요.');
    expect(stopTrack).toHaveBeenCalled();
  });

  test('컴포넌트 해제 시 녹음 스트림과 분석기를 정리한다', async () => {
    const { recorder, wrapper } = setup();
    await recorder.startRecording();

    wrapper.unmount();

    expect(recorder.isRecording.value).toBe(false);
    expect(stopTrack).toHaveBeenCalled();
    expect(mocks.stopAnalysis).toHaveBeenCalled();
    expect(mocks.stopPcm).toHaveBeenCalled();
  });
});
