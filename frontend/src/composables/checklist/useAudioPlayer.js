import {
  computed, nextTick, onBeforeUnmount, ref, watch,
} from 'vue';

export const useAudioPlayer = (props) => {
  const audioElement = ref(null);
  const currentTime = ref(0);
  const duration = ref(props.fallbackDuration);
  const isPlaying = ref(false);
  const progress = computed(() =>
    duration.value ? (currentTime.value / duration.value) * 100 : 0,
  );

  const togglePlayback = async () => {
    if (!audioElement.value) return;
    if (isPlaying.value) return audioElement.value.pause();

    try {
      await props.beforePlay?.();
      await nextTick();
      await audioElement.value.play();
    } catch {
      isPlaying.value = false;
    }
  };

  const updateMetadata = () => {
    const mediaDuration = audioElement.value?.duration;
    duration.value = Number.isFinite(mediaDuration) && mediaDuration > 0
      ? mediaDuration
      : props.fallbackDuration;
  };

  const updateCurrentTime = () => {
    currentTime.value = audioElement.value?.currentTime ?? 0;
  };

  const handleEnded = () => {
    isPlaying.value = false;
    currentTime.value = 0;
  };

  watch(() => props.src, () => {
    isPlaying.value = false;
    currentTime.value = 0;
    duration.value = props.fallbackDuration;
  });
  onBeforeUnmount(() => audioElement.value?.pause());

  return {
    audioElement, currentTime, duration, handleEnded, isPlaying,
    progress, togglePlayback, updateCurrentTime, updateMetadata,
  };
};
