<script setup>
import PauseIcon from '@/assets/icons/checklist/pause-mint-14.svg';
import PlayIcon from '@/assets/icons/checklist/play-mint-14.svg';
import { useAudioPlayer } from '@/composables/checklist/useAudioPlayer';
import { formatAudioTime } from '@/utils/audio';

const props = defineProps({
  src: { type: String, required: true },
  fallbackDuration: { type: Number, default: 0 },
});
const player = useAudioPlayer(props);
</script>

<template>
  <div class="recording-player d-flex align-items-center gap-2 w-100">
    <audio
      :ref="(element) => (player.audioElement.value = element)"
      :src="src"
      preload="metadata"
      @loadedmetadata="player.updateMetadata"
      @durationchange="player.updateMetadata"
      @timeupdate="player.updateCurrentTime"
      @play="player.isPlaying.value = true"
      @pause="player.isPlaying.value = false"
      @ended="player.handleEnded"
    ></audio>
    <button
      type="button"
      class="recording-player__toggle d-flex flex-shrink-0 align-items-center justify-content-center border-0 p-0"
      :aria-label="player.isPlaying.value ? '녹음 일시정지' : '녹음 재생'"
      @click="player.togglePlayback"
    >
      <img :src="player.isPlaying.value ? PauseIcon : PlayIcon" alt="" />
    </button>
    <span class="recording-player__time flex-shrink-0 fw-semibold">
      {{ formatAudioTime(player.currentTime.value) }}
    </span>
    <div
      class="recording-player__progress progress flex-grow-1"
      role="progressbar"
      aria-label="녹음 재생 진행률"
      aria-valuemin="0"
      aria-valuemax="100"
      :aria-valuenow="Math.round(player.progress.value)"
    >
      <div
        class="recording-player__bar progress-bar"
        :style="{ width: `${player.progress.value}%` }"
      ></div>
    </div>
    <span class="recording-player__time flex-shrink-0">
      {{ formatAudioTime(player.duration.value) }}
    </span>
  </div>
</template>

<style scoped>
.recording-player { min-width: 0; }
.recording-player__toggle {
  width: 30px;
  height: 30px;
  background-color: var(--mint-100);
  border-radius: 50%;
}

.recording-player__toggle img { width: 14px; height: 14px; }
.recording-player__time {
  color: var(--black-500);
  font-size: 0.6875rem;
  font-variant-numeric: tabular-nums;
}

.recording-player__progress {
  min-width: 32px;
  height: 6px;
  background-color: var(--black-100);
  border-radius: 999px;
}

.recording-player__bar {
  background-color: var(--mint-500);
  border-radius: inherit;
  transition: width 0.1s linear;
}
</style>
