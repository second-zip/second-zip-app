<script setup>
import { onMounted, ref } from 'vue';

import MemberSecretaryGuide from '@/components/common/secretary/MemberSecretaryGuide.vue';
import ReportCreateBox from '@/components/report/ReportCreateBox.vue';
import ReportDeleteModal from '@/components/report/list/ReportDeleteModal.vue';
import ReportListBox from '@/components/report/list/ReportListBox.vue';
import { useReportList } from '@/composables/report/useReportList';
import { REPORT_LIST_MESSAGE } from '@/constants/report/list';

const reportPendingDelete = ref(null);
const {
  reports,
  isLoading,
  errorMessage,
  isDeleting,
  deleteErrorMessage,
  fetchReports,
  toggleFavorite,
  removeReport,
  resetDeleteError,
} = useReportList();

const requestDelete = (report) => {
  resetDeleteError();
  reportPendingDelete.value = report;
};
const closeDeleteModal = () => {
  reportPendingDelete.value = null;
  resetDeleteError();
};
const confirmDelete = async () => {
  if (await removeReport(reportPendingDelete.value)) closeDeleteModal();
};

onMounted(fetchReports);
</script>

<template>
  <section class="report-list-view w-100 d-flex flex-column">
    <ReportCreateBox />
    <ReportListBox
      :reports="reports"
      :is-loading="isLoading"
      :error-message="errorMessage"
      @toggle-favorite="toggleFavorite"
      @delete="requestDelete"
    />
  </section>
  <MemberSecretaryGuide
    :messages="REPORT_LIST_MESSAGE"
    floating
  />
  <ReportDeleteModal
    :report="reportPendingDelete"
    :is-deleting="isDeleting"
    :error-message="deleteErrorMessage"
    @confirm="confirmDelete"
    @close="closeDeleteModal"
  />
</template>

<style scoped>
.report-list-view {
  min-height: 100%;
  padding: 20px;
  gap: 20px;
}
</style>
