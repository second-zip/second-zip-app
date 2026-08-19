<script setup>
defineProps({
  levels: {
    type: Array,
    default: () => [],
  },
});
</script>

<template>
  <div
    class="recording-waveform d-flex align-items-center justify-content-center gap-1"
    role="img"
    aria-label="현재 마이크 입력 음량"
  >
    <span
      v-for="(level, index) in levels"
      :key="index"
      :style="{ height: `${Math.max(3, Number(level) * 22)}px` }"
    ></span>
  </div>
</template>

<style scoped>
.recording-waveform {
  width: 64px;
  height: 24px;
}

.recording-waveform span {
  width: 3px;
  min-height: 3px;
  max-height: 22px;
  background-color: var(--mint-500);
  border-radius: 999px;
  transition: height 80ms linear;
}
</style>
