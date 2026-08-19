import { beforeEach, describe, expect, test, vi } from 'vitest';

import { usePcmAudioStream } from './usePcmAudioStream';

const makeConnectable = () => ({
  connect: vi.fn((target) => target),
  disconnect: vi.fn(),
});

class FakeAudioWorkletNode {
  constructor() {
    Object.assign(this, makeConnectable());
    this.port = {
      onmessage: null,
      postMessage: vi.fn(() => {
        this.port.onmessage?.({ data: { type: 'flushed' } });
      }),
    };
  }
}

const makeContext = (addModule = vi.fn().mockResolvedValue(undefined)) => ({
  audioWorklet: { addModule },
  close: vi.fn().mockResolvedValue(undefined),
  createGain: vi.fn(() => ({ ...makeConnectable(), gain: { value: 1 } })),
  createMediaStreamSource: vi.fn(makeConnectable),
  destination: {},
  resume: vi.fn().mockResolvedValue(undefined),
  state: 'running',
});

describe('usePcmAudioStream', () => {
  let context;

  beforeEach(() => {
    context = makeContext();
    window.AudioContext = vi.fn(() => context);
    window.AudioWorkletNode = FakeAudioWorkletNode;
  });

  test('AudioWorklet의 PCM chunk를 callback으로 전달하고 안전하게 종료한다', async () => {
    const onChunk = vi.fn();
    const pcm = usePcmAudioStream();
    await pcm.startPcmStream({}, onChunk);
    const node = window.AudioWorkletNode.mock?.results?.[0]?.value;

    // 생성자 mock이 아닌 class이므로 연결 대상에서 생성된 인스턴스를 찾는다.
    const workletNode = context.createMediaStreamSource.mock.results[0].value
      .connect.mock.calls[0][0];
    workletNode.port.onmessage({ data: new ArrayBuffer(8) });
    await pcm.stopPcmStream();

    expect(node).toBeUndefined();
    expect(onChunk).toHaveBeenCalledWith(expect.any(ArrayBuffer));
    expect(workletNode.port.postMessage).toHaveBeenCalledWith({ type: 'flush' });
    expect(context.close).toHaveBeenCalledOnce();
  });

  test('worklet 로딩 실패 후에도 생성된 AudioContext를 정리한다', async () => {
    context = makeContext(vi.fn().mockRejectedValue(new Error('load fail')));
    window.AudioContext = vi.fn(() => context);
    const pcm = usePcmAudioStream();

    await expect(pcm.startPcmStream({}, vi.fn())).rejects.toThrow('load fail');
    await expect(pcm.stopPcmStream()).resolves.toBeUndefined();
    expect(context.close).toHaveBeenCalledOnce();
  });
});
