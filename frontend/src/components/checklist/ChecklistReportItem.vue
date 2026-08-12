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
  isCreating: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['create']);

const hasChecklist = computed(() => Boolean(props.report.checklistCreated));
const progress = computed(() =>
  Math.min(100, Math.max(0, Number(props.report.percentage) || 0)),
);
const address = computed(() => formatReportAddress(props.report));
const formattedCreatedAt = computed(() =>
  formatReportDate(props.report.reportCreatedAt),
);
const createdAtDateTime = computed(() =>
  toReportDateTime(props.report.reportCreatedAt),
);
const detailRoute = computed(() => ({
  name: 'checklist-detail',
  params: { reportChecklistId: props.report.reportChecklistId },
}));
</script>

<template>
  <article class="checklist-report-item d-flex align-items-center w-100">
    <ReportListStatusIcon :result="report.result" />

    <div class="checklist-report-item__content flex-grow-1 overflow-hidden">
      <p class="checklist-report-item__address fw-semibold mb-0">
        {{ address }}
      </p>
      <time
        class="checklist-report-item__date fw-medium"
        :datetime="createdAtDateTime"
      >
        {{ formattedCreatedAt }}
      </time>
    </div>

    <RouterLink
      v-if="hasChecklist"
      :to="detailRoute"
      class="checklist-report-item__progress d-flex flex-shrink-0 align-items-center text-decoration-none"
      :aria-label="`${address} 체크리스트 상세 보기`"
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
    </RouterLink>

    <ChecklistCreateButton
      v-else
      :is-loading="isCreating"
      @create="emit('create', report)"
    />
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
