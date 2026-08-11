import { onBeforeUnmount, ref } from 'vue';

const EMPTY_LEVELS = Array.from({ length: 8 }, () => 0.08);

export const useAudioAnalyser = () => {
  const waveformLevels = ref([...EMPTY_LEVELS]);
  let analyser;
  let animationFrameId;
  let context;
  let source;

  const update = () => {
    if (!analyser) return;

    const samples = new Uint8Array(analyser.fftSize);
    analyser.getByteTimeDomainData(samples);
    const power = samples.reduce((sum, sample) => {
      const value = (sample - 128) / 128;
      return sum + value * value;
    }, 0);
    const volume = Math.min(
      1,
      Math.max(0.08, Math.sqrt(power / samples.length) * 4),
    );

    waveformLevels.value = [...waveformLevels.value.slice(1), volume];
    animationFrameId = window.requestAnimationFrame(update);
  };

  const start = (stream) => {
    const AudioContextClass = window.AudioContext ?? window.webkitAudioContext;
    if (!AudioContextClass) return;

    context = new AudioContextClass();
    analyser = context.createAnalyser();
    analyser.fftSize = 256;
    analyser.smoothingTimeConstant = 0.75;
    source = context.createMediaStreamSource(stream);
    source.connect(analyser);
    update();
  };

  const stop = () => {
    const closingContext = context;
    window.cancelAnimationFrame(animationFrameId);
    source?.disconnect();
    analyser = undefined;
    context = undefined;
    source = undefined;
    waveformLevels.value = [...EMPTY_LEVELS];
    if (closingContext?.state !== 'closed') void closingContext.close();
  };

  onBeforeUnmount(stop);
  return { startAnalysis: start, stopAnalysis: stop, waveformLevels };
};
