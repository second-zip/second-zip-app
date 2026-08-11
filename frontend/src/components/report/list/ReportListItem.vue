<script setup>
import { computed } from 'vue';

import ReportListItemActions from './ReportListItemActions.vue';
import ReportListStatusIcon from './ReportListStatusIcon.vue';
import { formatReportDate, toReportDateTime } from '@/utils/report/date';
import { formatReportAddress } from '@/utils/report/list';

const props = defineProps({
  report: { type: Object, required: true },
});
const emit = defineEmits(['toggle-favorite', 'delete']);
const address = computed(() => formatReportAddress(props.report));
const formattedCreatedAt = computed(() =>
  formatReportDate(props.report.createdAt),
);
const createdAtDateTime = computed(() =>
  toReportDateTime(props.report.createdAt),
);
</script>

<template>
  <article class="report-list-item d-flex align-items-center w-100">
    <ReportListStatusIcon :result="report.result" />
    <div class="report-list-item__content flex-grow-1 overflow-hidden">
      <p class="report-list-item__address fw-semibold">{{ address }}</p>
      <time
        class="report-list-item__date fw-medium"
        :datetime="createdAtDateTime"
      >
        {{ formattedCreatedAt }}
      </time>
    </div>
    <ReportListItemActions
      :report="report"
      :address="address"
      @toggle-favorite="emit('toggle-favorite', $event)"
      @delete="emit('delete', $event)"
    />
  </article>
</template>

<style scoped>
.report-list-item {
  min-height: 72px;
  padding: 16px;
  gap: 12px;
  border-bottom: 1px solid var(--black-100);
}

.report-list-item:last-child {
  border-bottom: 0;
}

.report-list-item__content {
  min-width: 0;
}

.report-list-item__address {
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--black-900);
  font-size: 0.875rem;
  line-height: 1.4;
}

.report-list-item__date {
  color: var(--black-300);
  font-size: 0.75rem;
}

@media (max-width: 360px) {
  .report-list-item {
    padding-inline: 12px;
    gap: 8px;
  }
}
</style>
