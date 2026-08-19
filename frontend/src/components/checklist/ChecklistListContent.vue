<script setup>
import ListBox from '@/components/common/ListBox.vue';
import ChecklistReportItem from './ChecklistReportItem.vue';

defineProps({
  checklists: { type: Array, default: () => [] },
  creatingReportIds: { type: Array, default: () => [] },
  isLoading: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
  creationErrorMessage: { type: String, default: '' },
});
defineEmits(['create']);
</script>

<template>
  <section class="checklist-list w-100">
    <ListBox
      title="리포트 목록"
      :items="checklists"
      item-key="analysisReportId"
      :is-loading="isLoading"
      :error-message="errorMessage"
      loading-message="체크리스트 목록을 불러오는 중입니다."
      empty-message="체크리스트를 만들 리포트가 없습니다."
    >
      <template #item="{ item }">
        <ChecklistReportItem
          :report="item"
          :is-creating="creatingReportIds.includes(item.analysisReportId)"
          @create="$emit('create', $event)"
        />
      </template>
    </ListBox>
    <p
      v-if="creationErrorMessage"
      class="checklist-list__error mt-2 mb-0 text-center"
      role="alert"
    >{{ creationErrorMessage }}</p>
  </section>
</template>

<style scoped>
.checklist-list {
  min-height: 100%;
  padding: 24px 20px;
}
.checklist-list__error {
  color: var(--red-500);
  font-size: 0.75rem;
}
</style>
