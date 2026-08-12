<script setup>
import ListBox from '@/components/common/ListBox.vue';
import { REPORT_LIST_FEEDBACK } from '@/constants/report/list';
import ReportListItem from './ReportListItem.vue';

defineProps({
  reports: {
    type: Array,
    default: () => [],
  },
  isLoading: {
    type: Boolean,
    default: false,
  },
  errorMessage: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['toggle-favorite', 'delete']);
</script>

<template>
  <ListBox
    title="리포트 목록"
    :items="reports"
    item-key="analysisReportId"
    :is-loading="isLoading"
    :error-message="errorMessage"
    :loading-message="REPORT_LIST_FEEDBACK.loading"
    :empty-message="REPORT_LIST_FEEDBACK.empty"
  >
    <template #item="{ item: report, index }">
      <slot name="item" :report="report" :index="index">
        <ReportListItem
          :report="report"
          @toggle-favorite="emit('toggle-favorite', $event)"
          @delete="emit('delete', $event)"
        />
      </slot>
    </template>
  </ListBox>
</template>
