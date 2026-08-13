import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, test, vi } from 'vitest';

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

class FakeMediaRecorder extends EventTarget {
  static instances = [];

  constructor(stream) {
    super();
    this.stream = stream;
    this.state = 'inactive';
    this.mimeType = 'audio/webm';
    FakeMediaRecorder.instances.push(this);
  }

  start(timeslice) {
    this.state = 'recording';
    this.timeslice = timeslice;
  }

  stop() {
    this.state = 'inactive';
    this.dispatchEvent(new Event('stop'));
  }

  emitData(data) {
    const event = new Event('dataavailable');
    Object.defineProperty(event, 'data', { value: data });
    this.dispatchEvent(event);
  }
}

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

  beforeEach(() => {
    vi.clearAllMocks();
    FakeMediaRecorder.instances = [];
    stream = { getTracks: () => [{ stop: vi.fn() }] };
    getUserMedia = vi.fn().mockResolvedValue(stream);
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true, value: { getUserMedia },
    });
    vi.stubGlobal('MediaRecorder', FakeMediaRecorder);
    mocks.startPcm.mockResolvedValue(undefined);
    mocks.stopPcm.mockResolvedValue(undefined);
  });

  test('마이크·분석·PCM 전송을 시작하고 로컬 녹음 Blob을 만든다', async () => {
    const { recorder } = setup();
    const beforeStart = vi.fn();
    const onPcmChunk = vi.fn();

    await recorder.startRecording({ beforeStart, onPcmChunk });
    const mediaRecorder = FakeMediaRecorder.instances[0];
    mediaRecorder.emitData(new Blob(['audio']));
    const blob = await recorder.stopRecording();

    expect(getUserMedia).toHaveBeenCalledWith({ audio: {
      channelCount: { ideal: 1 }, sampleRate: { ideal: 16000 },
    } });
    expect(beforeStart).toHaveBeenCalledBefore(mocks.startPcm);
    expect(mocks.startPcm).toHaveBeenCalledWith(stream, onPcmChunk);
    expect(mediaRecorder.timeslice).toBe(1000);
    expect(blob.type).toBe('audio/webm');
    expect(blob.size).toBeGreaterThan(0);
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
});
