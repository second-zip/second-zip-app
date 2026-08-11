<script setup>
import PlusIcon from '@/assets/icons/report/plus-white-14.svg';

defineProps({
  isLoading: { type: Boolean, default: false },
});
const emit = defineEmits(['create']);
</script>

<template>
  <button
    type="button"
    class="checklist-create-button d-inline-flex flex-shrink-0 align-items-center justify-content-center border-0 fw-semibold"
    :disabled="isLoading"
    @click="emit('create')"
  >
    <img
      v-if="!isLoading"
      :src="PlusIcon"
      class="checklist-create-button__icon"
      alt=""
    />
    <span>{{ isLoading ? '생성 중' : '생성' }}</span>
  </button>
</template>

<style scoped>
.checklist-create-button {
  width: fit-content;
  height: 28px;
  padding: 0 12px;
  gap: 4px;
  color: white;
  background-color: var(--blue-900);
  border-radius: 999px;
  font-size: 0.75rem;
  line-height: 1;
  white-space: nowrap;
}

.checklist-create-button__icon {
  width: 14px;
  height: 14px;
}

.checklist-create-button:focus-visible {
  outline: 2px solid var(--blue-500);
  outline-offset: 2px;
}

.checklist-create-button:disabled { opacity: 0.6; }
</style>
