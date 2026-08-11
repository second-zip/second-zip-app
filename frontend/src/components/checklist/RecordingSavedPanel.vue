<script setup>
import RecordingPlayer from './RecordingPlayer.vue';

defineProps({
  recording: { type: Object, required: true },
});
defineEmits(['delete', 'show-text']);
</script>

<template>
  <div class="recording-saved d-flex flex-column">
    <RecordingPlayer
      :src="recording.url"
      :fallback-duration="recording.duration"
    />
    <div class="recording-saved__actions d-flex gap-2">
      <button
        type="button"
        class="recording-saved__delete btn flex-grow-1 fw-semibold"
        @click="$emit('delete')"
      >녹음 삭제</button>
      <button
        type="button"
        class="recording-saved__text btn flex-grow-1 fw-semibold"
        @click="$emit('show-text')"
      >텍스트 읽기</button>
    </div>
  </div>
</template>

<style scoped>
.recording-saved__actions { margin-top: 8px; }
.recording-saved__actions .btn {
  height: 28px;
  padding: 0 10px;
  border-radius: 10px;
  font-size: 0.6875rem;
}

.recording-saved__delete {
  color: var(--red-500);
  background-color: white;
  border-color: var(--black-100);
}

.recording-saved__text {
  color: var(--black-700);
  background-color: var(--mint-100);
  border-color: var(--mint-500);
}
</style>
