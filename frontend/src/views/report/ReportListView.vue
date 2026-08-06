<script setup>
import { computed, onMounted, ref } from 'vue';

import ReportCreateBox from '@/components/report/list/ReportCreateBox.vue';
import ReportDeleteModal from '@/components/report/list/ReportDeleteModal.vue';
import ReportListBox from '@/components/report/list/ReportListBox.vue';
import SecretaryGuide from '@/components/common/secretary/SecretaryGuide.vue';
import { useReportList } from '@/composables/report/useReportList';
import {
  REPORT_CHARACTER_TYPES,
  REPORT_LIST_MESSAGE,
} from '@/constants/report/list';
import { useAuthStore } from '@/stores/auth';
import { logger } from '@/utils/logger';

const authStore = useAuthStore();
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

const characterType = computed(() => {
  if (!authStore.isAuthenticated) return 'CAT';
  const type = authStore.myPage?.characterType;
  return REPORT_CHARACTER_TYPES.has(type) ? type : 'CAT';
});
const secretaryMessage = computed(
  () => REPORT_LIST_MESSAGE[characterType.value],
);
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
onMounted(async () => {
  if (!authStore.isAuthenticated || authStore.myPage) return;
  try {
    await authStore.fetchMyPage();
  } catch (error) {
    logger.error('report-list.fetch-user', error);
  }
});
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
  <SecretaryGuide
    v-if="!authStore.loading"
    :text="secretaryMessage"
    :character-type="characterType"
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
