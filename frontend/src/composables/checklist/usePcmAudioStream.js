const workletUrl = new URL(
  '../../worklets/pcmCaptureProcessor.js',
  import.meta.url,
);

export const usePcmAudioStream = () => {
  let context;
  let node;
  let source;
  let silentGain;
  let flushResolver;

  const start = async (stream, onChunk) => {
    await stop();
    const AudioContextClass = window.AudioContext ?? window.webkitAudioContext;
    context = new AudioContextClass({
      latencyHint: 'interactive',
      sampleRate: 16000,
    });
    await context.audioWorklet.addModule(workletUrl);
    source = context.createMediaStreamSource(stream);
    node = new window.AudioWorkletNode(context, 'pcm-capture-processor');
    silentGain = context.createGain();
    silentGain.gain.value = 0;
    node.port.onmessage = ({ data }) => {
      if (data?.type === 'flushed') {
        flushResolver?.();
        flushResolver = undefined;
      } else if (data instanceof ArrayBuffer && data.byteLength) {
        onChunk(data);
      }
    };
    source.connect(node).connect(silentGain).connect(context.destination);
    await context.resume();
  };

  const stop = async () => {
    const activeNode = node;
    if (!activeNode && !context) return;
    if (activeNode) {
      await Promise.race([
        new Promise((resolve) => {
          flushResolver = resolve;
          activeNode.port.postMessage({ type: 'flush' });
        }),
        new Promise((resolve) => window.setTimeout(resolve, 500)),
      ]);
    }
    source?.disconnect();
    activeNode.disconnect();
    silentGain?.disconnect();
    const activeContext = context;
    source = undefined;
    node = undefined;
    silentGain = undefined;
    context = undefined;
    flushResolver = undefined;
    if (activeContext?.state !== 'closed') await activeContext.close();
  };

  return { startPcmStream: start, stopPcmStream: stop };
};
