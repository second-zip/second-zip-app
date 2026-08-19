import { beforeAll, beforeEach, describe, expect, test, vi } from 'vitest';

let Processor;

class FakeAudioWorkletProcessor {
  constructor() {
    this.port = { onmessage: null, postMessage: vi.fn() };
  }
}

describe('PcmCaptureProcessor', () => {
  beforeAll(async () => {
    vi.stubGlobal('sampleRate', 16000);
    vi.stubGlobal('AudioWorkletProcessor', FakeAudioWorkletProcessor);
    vi.stubGlobal('registerProcessor', (_, registered) => {
      Processor = registered;
    });
    await import('./pcmCaptureProcessor');
  });

  beforeEach(() => vi.clearAllMocks());

  test('250ms 분량을 8000 byte PCM16 little-endian chunk로 보낸다', () => {
    const processor = new Processor();
    processor.process([[
      Float32Array.from({ length: 4001 }, () => 1),
    ]]);

    const [buffer, transfer] = processor.port.postMessage.mock.calls[0];
    expect(buffer).toBeInstanceOf(ArrayBuffer);
    expect(buffer.byteLength).toBe(8000);
    expect(transfer).toEqual([buffer]);
    expect(new DataView(buffer).getInt16(0, true)).toBe(32767);
  });

  test('여러 채널을 mono로 평균하고 signed 16-bit로 변환한다', () => {
    const processor = new Processor();
    processor.process([[
      new Float32Array([1, 1, 1]),
      new Float32Array([-1, -1, -1]),
    ]]);
    processor.port.onmessage({ data: { type: 'flush' } });

    const buffer = processor.port.postMessage.mock.calls[0][0];
    expect([...new Int16Array(buffer)]).toEqual([0, 0]);
    expect(processor.port.postMessage).toHaveBeenLastCalledWith({
      type: 'flushed',
    });
  });

  test('입력 샘플을 PCM 범위 안으로 제한한다', () => {
    const processor = new Processor();
    processor.process([[
      new Float32Array([2, -2, 0]),
    ]]);
    processor.port.onmessage({ data: { type: 'flush' } });

    const view = new DataView(processor.port.postMessage.mock.calls[0][0]);
    expect(view.getInt16(0, true)).toBe(32767);
    expect(view.getInt16(2, true)).toBe(-32768);
  });
});
