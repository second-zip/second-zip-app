<script setup>
import ContentBox from '@/components/common/ContentBox.vue';
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
  <div class="report-list-box w-100 d-flex flex-column">
    <div class="list-title d-flex align-items-center">
      <h3 class="fw-bold fs-6">리포트 목록</h3>
      <span class="fw-semibold">{{ reports.length }}건</span>
    </div>
    <ContentBox flush>
      <p v-if="isLoading" class="list-feedback mb-0" role="status">
        {{ REPORT_LIST_FEEDBACK.loading }}
      </p>
      <p
        v-else-if="errorMessage"
        class="list-feedback list-feedback--error mb-0"
        role="alert"
      >
        {{ errorMessage }}
      </p>
      <p v-else-if="reports.length === 0" class="list-feedback mb-0">
        {{ REPORT_LIST_FEEDBACK.empty }}
      </p>
      <template v-else>
        <template
          v-for="(report, index) in reports"
          :key="report.analysisReportId"
        >
          <slot name="item" :report="report" :index="index">
            <ReportListItem
              :report="report"
              @toggle-favorite="emit('toggle-favorite', $event)"
              @delete="emit('delete', $event)"
            />
          </slot>
        </template>
      </template>
    </ContentBox>
  </div>
</template>

<style scoped>
.report-list-box {
  height: fit-content;
  gap: 8px;
}

.list-title {
  height: fit-content;
  gap: 8px;
}

.list-title h3 {
  height: fit-content;
  margin: 0;
  color: var(--black-900);
}

.list-title span {
  margin: 0;
  background-color: var(--black-100);
  color: var(--black-500);
  border-radius: 10px;
  font-size: 0.75rem;
  padding: 3px 10px;
}

.list-feedback {
  padding: 28px 16px;
  color: var(--black-500);
  font-size: 0.875rem;
  text-align: center;
}

.list-feedback--error {
  color: var(--red-500);
}
</style>
