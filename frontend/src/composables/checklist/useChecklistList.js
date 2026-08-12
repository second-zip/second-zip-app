import { ref } from 'vue';

import {
  createChecklist as requestChecklistCreation,
  getChecklists,
} from '@/api/checklist';
import { getApiError } from '@/api/utils/error';

export const useChecklistList = () => {
  const checklists = ref([]);
  const isLoading = ref(false);
  const errorMessage = ref('');
  const creationErrorMessage = ref('');
  const creatingReportIds = ref([]);

  const fetchChecklists = async () => {
    isLoading.value = true;
    errorMessage.value = '';

    try {
      const data = await getChecklists();
      checklists.value = Array.isArray(data) ? data : [];
    } catch (error) {
      errorMessage.value = getApiError(error).message;
    } finally {
      isLoading.value = false;
    }
  };

  const createChecklist = async (report) => {
    const analysisReportId = report?.analysisReportId;

    if (
      !analysisReportId ||
      report.checklistCreated ||
      creatingReportIds.value.includes(analysisReportId)
    ) return false;

    creatingReportIds.value = [
      ...creatingReportIds.value,
      analysisReportId,
    ];
    creationErrorMessage.value = '';

    try {
      const data = await requestChecklistCreation(analysisReportId);

      const createdChecklist = {
        ...report,
        checklistCreated: true,
        reportChecklistId: data?.reportChecklistId,
        percentage: 0,
      };
      checklists.value = checklists.value.map((item) =>
        item.analysisReportId === analysisReportId
          ? createdChecklist
          : item,
      );
      return createdChecklist;
    } catch (error) {
      creationErrorMessage.value = getApiError(error).message;
      return false;
    } finally {
      creatingReportIds.value = creatingReportIds.value.filter(
        (id) => id !== analysisReportId,
      );
    }
  };

  return {
    checklists,
    creatingReportIds,
    isLoading,
    errorMessage,
    creationErrorMessage,
    fetchChecklists,
    createChecklist,
  };
};
