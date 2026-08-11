<script setup>
import { computed } from 'vue';

const props = defineProps({
  completedCount: {
    type: Number,
    default: 0,
  },
  totalCount: {
    type: Number,
    default: 0,
  },
  progress: {
    type: Number,
    default: 0,
  },
});

const progressWidth = computed(() =>
  Math.min(100, Math.max(0, Number(props.progress) || 0)),
);
</script>

<template>
  <section class="checklist-progress w-100" aria-labelledby="progress-title">
    <div class="d-flex align-items-center justify-content-between">
      <h2 id="progress-title" class="fw-semibold mb-0">완료 현황</h2>
      <strong class="checklist-progress__count">
        {{ completedCount }} / {{ totalCount }}
      </strong>
    </div>

    <div
      class="checklist-progress__track w-100 overflow-hidden"
      role="progressbar"
      aria-label="체크리스트 완료율"
      aria-valuemin="0"
      aria-valuemax="100"
      :aria-valuenow="Math.round(progressWidth)"
    >
      <div
        class="checklist-progress__bar h-100"
        :style="{ width: `${progressWidth}%` }"
      ></div>
    </div>
  </section>
</template>

<style scoped>
.checklist-progress h2,
.checklist-progress__count {
  font-size: 0.875rem;
}

.checklist-progress h2 {
  color: var(--black-700);
}

.checklist-progress__count {
  color: var(--blue-900);
}

.checklist-progress__track {
  height: 8px;
  margin-top: 12px;
  background-color: var(--black-100);
  border-radius: 999px;
}

.checklist-progress__bar {
  background-color: var(--blue-900);
  border-radius: inherit;
  transition: width 0.25s ease;
}
</style>
