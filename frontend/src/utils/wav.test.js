import { describe, expect, test } from 'vitest';

import { createPcmWavBlob } from './wav';

const readAscii = (view, offset, length) => Array.from(
  { length },
  (_, index) => String.fromCharCode(view.getUint8(offset + index)),
).join('');
const readBlob = (blob) => new Promise((resolve, reject) => {
  const reader = new FileReader();
  reader.addEventListener('load', () => resolve(reader.result), { once: true });
  reader.addEventListener('error', reject, { once: true });
  reader.readAsArrayBuffer(blob);
});

describe('createPcmWavBlob', () => {
  test('16kHz mono PCM16 청크에 올바른 WAV 헤더를 붙인다', async () => {
    const first = Uint8Array.from([0xe8, 0x03]).buffer;
    const second = Uint8Array.from([0x18, 0xfc]).buffer;

    const blob = createPcmWavBlob([first, second]);
    const view = new DataView(await readBlob(blob));

    expect(blob.type).toBe('audio/wav');
    expect(blob.size).toBe(48);
    expect(readAscii(view, 0, 4)).toBe('RIFF');
    expect(view.getUint32(4, true)).toBe(40);
    expect(readAscii(view, 8, 4)).toBe('WAVE');
    expect(view.getUint16(20, true)).toBe(1);
    expect(view.getUint16(22, true)).toBe(1);
    expect(view.getUint32(24, true)).toBe(16000);
    expect(view.getUint16(34, true)).toBe(16);
    expect(readAscii(view, 36, 4)).toBe('data');
    expect(view.getUint32(40, true)).toBe(4);
    expect(view.getInt16(44, true)).toBe(1000);
    expect(view.getInt16(46, true)).toBe(-1000);
  });

  test('PCM 데이터가 없으면 파일을 만들지 않는다', () => {
    expect(createPcmWavBlob([])).toBeNull();
  });
});
