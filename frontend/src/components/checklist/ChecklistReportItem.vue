<script setup>
import { computed } from 'vue';

import ChevronRightIcon from '@/assets/icons/report/arrow-gray-14.svg';
import ReportListStatusIcon from '@/components/report/list/ReportListStatusIcon.vue';
import { formatReportDate, toReportDateTime } from '@/utils/report/date';
import { formatReportAddress } from '@/utils/report/list';
import ChecklistCreateButton from './ChecklistCreateButton.vue';
import CircularProgress from './CircularProgress.vue';

const props = defineProps({
  report: {
    type: Object,
    required: true,
  },
  index: {
    type: Number,
    required: true,
  },
});

const emit = defineEmits(['create']);

const isCreationInProgress = computed(() => props.index % 2 === 1);
const progress = 20;
const address = computed(() => formatReportAddress(props.report));
const formattedCreatedAt = computed(() =>
  formatReportDate(props.report.createdAt),
);
const createdAtDateTime = computed(() =>
  toReportDateTime(props.report.createdAt),
);
</script>

<template>
  <article class="checklist-report-item d-flex align-items-center w-100">
    <ReportListStatusIcon :result="report.result" />

    <div class="checklist-report-item__content flex-grow-1 overflow-hidden">
      <p
        class="checklist-report-item__address fw-semibold mb-0"
      >
        {{ address }}
      </p>
      <time
        class="checklist-report-item__date fw-medium"
        :datetime="createdAtDateTime"
      >
        {{ formattedCreatedAt }}
      </time>
    </div>

    <div
      v-if="isCreationInProgress"
      class="checklist-report-item__progress d-flex flex-shrink-0 align-items-center"
    >
      <CircularProgress :value="progress" :size="20" />
      <span class="checklist-report-item__percent fw-semibold">
        {{ progress }}%
      </span>
      <img
        :src="ChevronRightIcon"
        class="checklist-report-item__chevron"
        alt=""
      />
    </div>

    <ChecklistCreateButton v-else @create="emit('create', report)" />
  </article>
</template>

<style scoped>
.checklist-report-item {
  min-height: 80px;
  padding: 16px;
  gap: 12px;
  border-bottom: 1px solid var(--black-100);
}

.checklist-report-item:last-child {
  border-bottom: 0;
}

.checklist-report-item__content {
  min-width: 0;
}

.checklist-report-item__address {
  overflow-wrap: anywhere;
  color: var(--black-900);
  font-size: 0.875rem;
  line-height: 1.4;
}

.checklist-report-item__date {
  color: var(--black-300);
  font-size: 0.75rem;
  line-height: 1.4;
}

.checklist-report-item__progress {
  gap: 6px;
}

.checklist-report-item__percent {
  color: var(--blue-900);
  font-size: 0.75rem;
  white-space: nowrap;
}

.checklist-report-item__chevron {
  width: 14px;
  height: 14px;
}

@media (max-width: 360px) {
  .checklist-report-item {
    padding-inline: 12px;
    gap: 8px;
  }

  .checklist-report-item__progress {
    gap: 4px;
  }
}
</style>
