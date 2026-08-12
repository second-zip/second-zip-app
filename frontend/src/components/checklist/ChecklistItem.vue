<script setup>
import { computed } from 'vue';

import CheckIcon from '@/assets/icons/checklist/check-white-14.svg';
import QuestionIcon from '@/assets/icons/question-gray-16.svg';

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(['toggle', 'show-description']);
const inputId = computed(() => `checklist-item-${props.item.id}`);
</script>

<template>
  <article
    class="checklist-item d-flex align-items-center w-100"
    :class="{ 'checklist-item--checked': item.checked }"
  >
    <label
      :for="inputId"
      class="checklist-item__label d-flex align-items-center flex-grow-1"
    >
      <input
        :id="inputId"
        class="visually-hidden"
        type="checkbox"
        :checked="item.checked"
        @change="emit('toggle')"
      />

      <span
        class="checklist-item__checkbox d-flex flex-shrink-0 align-items-center justify-content-center"
        :class="{ 'checklist-item__checkbox--checked': item.checked }"
        aria-hidden="true"
      >
        <img v-if="item.checked" :src="CheckIcon" alt="" />
      </span>

      <span class="checklist-item__title flex-grow-1 fw-semibold">
        {{ item.title }}
      </span>
    </label>

    <button
      type="button"
      class="checklist-item__help d-flex flex-shrink-0 align-items-center justify-content-center border-0 p-0 bg-transparent"
      :aria-label="`${item.title} 항목 설명 보기`"
      @click.stop="emit('show-description')"
    >
      <img :src="QuestionIcon" alt="" />
    </button>
  </article>
</template>

<style scoped>
.checklist-item {
  height: 56px;
  padding: 18px;
  background-color: white;
  border: 2px solid var(--black-100);
  border-radius: 18px;
  box-shadow: 0 2px 10px rgb(17 17 24 / 6%);
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.checklist-item--checked {
  background-color: var(--blue-100);
  border-color: var(--blue-700);
}

.checklist-item__label {
  min-width: 0;
  gap: 14px;
  cursor: pointer;
}

.checklist-item__checkbox {
  width: 26px;
  height: 26px;
  background-color: white;
  border: 2px solid var(--black-100);
  border-radius: 8px;
}

.checklist-item__checkbox--checked {
  background-color: var(--blue-900);
  border-color: var(--blue-900);
  box-shadow: 0 4px 10px rgb(7 94 241 / 24%);
}

.checklist-item__checkbox img {
  width: 14px;
  height: 14px;
}

.checklist-item__title {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--black-900);
  font-size: 0.875rem;
  line-height: 1.4;
}

.checklist-item--checked .checklist-item__title {
  color: var(--blue-900);
  text-decoration: line-through;
  text-decoration-color: var(--blue-900);
  text-decoration-thickness: 1.5px;
}

.checklist-item__help {
  width: 16px;
  height: 16px;
  border-radius: 50%;
}

.checklist-item__help img {
  width: 16px;
  height: 16px;
}

.checklist-item__help:focus-visible,
.checklist-item__label:has(input:focus-visible) {
  outline: 2px solid var(--blue-500);
  outline-offset: 2px;
}

@media (max-width: 360px) {
  .checklist-item {
    padding-left: 14px;
  }

  .checklist-item__label {
    gap: 10px !important;
  }
}
</style>
