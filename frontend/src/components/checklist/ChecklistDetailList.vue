<script setup>
import ChecklistItem from './ChecklistItem.vue';

defineProps({
  items: { type: Array, default: () => [] },
  isLoading: { type: Boolean, default: false },
  loadErrorMessage: { type: String, default: '' },
  actionErrorMessage: { type: String, default: '' },
});
defineEmits(['toggle', 'show-description']);

const isCommon = (item) => item.category === 'COMMON';
const shouldShowLabel = (items, index) =>
  index === 0 || isCommon(items[index - 1]) !== isCommon(items[index]);
</script>

<template>
  <section class="checklist-detail-list d-flex flex-column mt-4">
    <p v-if="isLoading" class="checklist-detail-list__feedback mb-0" role="status">
      체크리스트를 불러오는 중입니다.
    </p>
    <p
      v-else-if="loadErrorMessage"
      class="checklist-detail-list__feedback checklist-detail-list__error mb-0"
      role="alert"
    >{{ loadErrorMessage }}</p>
    <p v-else-if="items.length === 0" class="checklist-detail-list__feedback mb-0">
      확인할 체크리스트 항목이 없습니다.
    </p>
    <template v-else>
      <template v-for="(item, index) in items" :key="item.id">
        <h2
          v-if="shouldShowLabel(items, index)"
          class="checklist-detail-list__label mb-0 fw-semibold"
          :class="{ 'mt-2': index > 0 }"
        >{{ isCommon(item) ? '공통' : '유형별' }}</h2>
        <ChecklistItem
          :item="item"
          @toggle="$emit('toggle', item.id)"
          @show-description="$emit('show-description', item)"
        />
      </template>
    </template>
    <p
      v-if="actionErrorMessage"
      class="checklist-detail-list__error mb-0 text-center"
      role="alert"
    >{{ actionErrorMessage }}</p>
  </section>
</template>

<style scoped>
.checklist-detail-list { gap: 12px; }
.checklist-detail-list__label {
  color: var(--black-500);
  font-size: 0.8125rem;
}
.checklist-detail-list__feedback {
  padding: 28px 16px;
  color: var(--black-500);
  font-size: 0.875rem;
  text-align: center;
}
.checklist-detail-list__error {
  color: var(--red-500);
  font-size: 0.75rem;
}
</style>
