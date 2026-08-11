<script setup>
import ChecklistLogoIcon from '@/assets/icons/checklist/checklogo-blue-18.svg';

defineProps({
  address: {
    type: String,
    required: true,
  },
});

const emit = defineEmits(['reset']);
</script>

<template>
  <header class="checklist-detail-header w-100">
    <div class="d-flex align-items-center justify-content-between gap-3">
      <div
        class="checklist-detail-header__address d-flex align-items-center flex-grow-1 gap-2"
      >
        <img :src="ChecklistLogoIcon" class="flex-shrink-0" alt="" />
        <h1 class="text-truncate mb-0">{{ address }}</h1>
      </div>

      <button
        type="button"
        class="checklist-detail-header__reset flex-shrink-0 border-0 fw-semibold"
        @click="emit('reset')"
      >
        초기화
      </button>
    </div>
  </header>
</template>

<style scoped>
.checklist-detail-header__eyebrow {
  color: var(--black-300);
  font-size: 0.75rem;
}

.checklist-detail-header__address {
  min-width: 0;
}

.checklist-detail-header__address img {
  width: 18px;
  height: 18px;
}

.checklist-detail-header__address h1 {
  overflow: hidden;
  font-size: 0.875rem;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.checklist-detail-header__reset {
  height: 28px;
  padding: 0 16px;
  color: white;
  background-color: var(--blue-900);
  border-radius: 999px;
  font-size: 0.8125rem;
}

.checklist-detail-header__reset:focus-visible {
  outline: 2px solid var(--blue-500);
  outline-offset: 2px;
}
</style>
