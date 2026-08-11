import { ref } from 'vue';

import {
  addReportFavorite,
  deleteReport,
  deleteReportFavorite,
  getReports,
} from '@/api/report';
import { getApiError } from '@/api/utils/error';
import { logger } from '@/utils/logger';
import {
  sortReportsByFavorite,
  updateFavoriteReport,
} from '@/utils/report/list';

export const useReportList = () => {
  const reports = ref([]);
  const isLoading = ref(false);
  const errorMessage = ref('');
  const isDeleting = ref(false);
  const deleteErrorMessage = ref('');

  const fetchReports = async () => {
    isLoading.value = true;
    errorMessage.value = '';

    try {
      const data = await getReports();
      reports.value = sortReportsByFavorite(
        Array.isArray(data.reports) ? data.reports : [],
      );
    } catch (error) {
      errorMessage.value = getApiError(error).message;
    } finally {
      isLoading.value = false;
    }
  };

  const toggleFavorite = async (report) => {
    try {
      const request = report.favorite
        ? deleteReportFavorite
        : addReportFavorite;

      await request(report.analysisReportId);
      reports.value = updateFavoriteReport(reports.value, report);
    } catch (error) {
      logger.error('report-list.toggle-favorite', error, {
        analysisReportId: report.analysisReportId,
      });
    }
  };

  const removeReport = async (report) => {
    if (!report || isDeleting.value) return false;

    isDeleting.value = true;
    deleteErrorMessage.value = '';

    try {
      await deleteReport(report.analysisReportId);
      reports.value = reports.value.filter(
        (item) => item.analysisReportId !== report.analysisReportId,
      );
      return true;
    } catch (error) {
      deleteErrorMessage.value = getApiError(error).message;
      logger.error('report-list.delete', error, {
        analysisReportId: report.analysisReportId,
      });
      return false;
    } finally {
      isDeleting.value = false;
    }
  };

  const resetDeleteError = () => {
    deleteErrorMessage.value = '';
  };

  return {
    reports,
    isLoading,
    errorMessage,
    isDeleting,
    deleteErrorMessage,
    fetchReports,
    toggleFavorite,
    removeReport,
    resetDeleteError,
  };
};
