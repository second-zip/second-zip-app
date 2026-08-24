const WAV_HEADER_SIZE = 44;
const PCM_SAMPLE_RATE = 16000;
const PCM_CHANNEL_COUNT = 1;
const PCM_BITS_PER_SAMPLE = 16;

const writeAscii = (view, offset, value) => {
  for (let index = 0; index < value.length; index += 1) {
    view.setUint8(offset + index, value.charCodeAt(index));
  }
};

export const createPcmWavBlob = (pcmChunks) => {
  const dataSize = pcmChunks.reduce(
    (total, chunk) => total + chunk.byteLength,
    0,
  );
  if (!dataSize) return null;

  const header = new ArrayBuffer(WAV_HEADER_SIZE);
  const view = new DataView(header);
  const bytesPerSample = PCM_BITS_PER_SAMPLE / 8;
  const blockAlign = PCM_CHANNEL_COUNT * bytesPerSample;
  const byteRate = PCM_SAMPLE_RATE * blockAlign;

  writeAscii(view, 0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeAscii(view, 8, 'WAVE');
  writeAscii(view, 12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, PCM_CHANNEL_COUNT, true);
  view.setUint32(24, PCM_SAMPLE_RATE, true);
  view.setUint32(28, byteRate, true);
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, PCM_BITS_PER_SAMPLE, true);
  writeAscii(view, 36, 'data');
  view.setUint32(40, dataSize, true);

  return new Blob([header, ...pcmChunks], { type: 'audio/wav' });
};
