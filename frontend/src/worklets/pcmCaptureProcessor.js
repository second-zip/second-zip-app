const TARGET_SAMPLE_RATE = 16000;
const CHUNK_SAMPLE_COUNT = 4000;

class PcmCaptureProcessor extends AudioWorkletProcessor {
  constructor() {
    super();
    this.pending = new Float32Array(0);
    this.readOffset = 0;
    this.chunk = new Int16Array(CHUNK_SAMPLE_COUNT);
    this.chunkOffset = 0;
    this.port.onmessage = ({ data }) => {
      if (data?.type !== 'flush') return;
      this.emitChunk();
      this.port.postMessage({ type: 'flushed' });
    };
  }

  appendSamples(inputChannels) {
    const frameCount = inputChannels[0]?.length ?? 0;
    if (!frameCount) return;
    const mono = new Float32Array(frameCount);
    for (const channel of inputChannels) {
      for (let index = 0; index < frameCount; index += 1) {
        mono[index] += channel[index] / inputChannels.length;
      }
    }
    const combined = new Float32Array(this.pending.length + mono.length);
    combined.set(this.pending);
    combined.set(mono, this.pending.length);
    this.pending = combined;
  }

  resample() {
    const ratio = sampleRate / TARGET_SAMPLE_RATE;
    while (this.readOffset + 1 < this.pending.length) {
      const leftIndex = Math.floor(this.readOffset);
      const fraction = this.readOffset - leftIndex;
      const sample = this.pending[leftIndex]
        + (this.pending[leftIndex + 1] - this.pending[leftIndex]) * fraction;
      this.writeSample(sample);
      this.readOffset += ratio;
    }
    const consumed = Math.floor(this.readOffset);
    this.pending = this.pending.slice(consumed);
    this.readOffset -= consumed;
  }

  writeSample(sample) {
    const clamped = Math.max(-1, Math.min(1, sample));
    this.chunk[this.chunkOffset] = Math.round(
      clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff,
    );
    this.chunkOffset += 1;
    if (this.chunkOffset === this.chunk.length) this.emitChunk();
  }

  emitChunk() {
    if (!this.chunkOffset) return;
    const buffer = new ArrayBuffer(this.chunkOffset * Int16Array.BYTES_PER_ELEMENT);
    const view = new DataView(buffer);
    for (let index = 0; index < this.chunkOffset; index += 1) {
      view.setInt16(index * Int16Array.BYTES_PER_ELEMENT, this.chunk[index], true);
    }
    this.port.postMessage(buffer, [buffer]);
    this.chunk = new Int16Array(CHUNK_SAMPLE_COUNT);
    this.chunkOffset = 0;
  }

  process(inputs) {
    if (inputs[0]?.length) {
      this.appendSamples(inputs[0]);
      this.resample();
    }
    return true;
  }
}

registerProcessor('pcm-capture-processor', PcmCaptureProcessor);
