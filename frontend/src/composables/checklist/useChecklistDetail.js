import { computed, ref, toValue } from 'vue';

import {
  getChecklist,
  resetChecklist,
  toggleChecklistItem,
} from '@/api/checklist';
import { getApiError } from '@/api/utils/error';
import { formatReportAddress } from '@/utils/report/list';

const normalizeItem = (item) => ({
  ...item,
  id: item.checklistItemId,
  title: item.contents,
  description: item.description,
});

export const useChecklistDetail = (reportChecklistId) => {
  const address = ref('-');
  const items = ref([]);
  const isLoading = ref(false);
  const isResetting = ref(false);
  const pendingItemIds = ref([]);
  const loadErrorMessage = ref('');
  const actionErrorMessage = ref('');
  const completedCount = computed(
    () => items.value.filter((item) => item.checked).length,
  );
  const progress = computed(() =>
    items.value.length
      ? (completedCount.value / items.value.length) * 100
      : 0,
  );

  const fetchChecklist = async () => {
    isLoading.value = true;
    loadErrorMessage.value = '';
    try {
      const data = await getChecklist(toValue(reportChecklistId));
      address.value = formatReportAddress(data);
      items.value = Array.isArray(data?.items)
        ? data.items.map(normalizeItem)
        : [];
    } catch (error) {
      loadErrorMessage.value = getApiError(error).message;
    } finally {
      isLoading.value = false;
    }
  };

  const toggleItem = async (id) => {
    const item = items.value.find((entry) => entry.id === id);
    if (
      !item ||
      isResetting.value ||
      pendingItemIds.value.includes(id)
    ) return;

    pendingItemIds.value = [...pendingItemIds.value, id];
    actionErrorMessage.value = '';
    try {
      const checked = !item.checked;
      await toggleChecklistItem(toValue(reportChecklistId), id, checked);
      item.checked = checked;
    } catch (error) {
      actionErrorMessage.value = getApiError(error).message;
    } finally {
      pendingItemIds.value = pendingItemIds.value.filter(
        (itemId) => itemId !== id,
      );
    }
  };

  const resetItems = async () => {
    if (isResetting.value || pendingItemIds.value.length) return;
    isResetting.value = true;
    actionErrorMessage.value = '';
    try {
      await resetChecklist(toValue(reportChecklistId));
      items.value.forEach((item) => { item.checked = false; });
    } catch (error) {
      actionErrorMessage.value = getApiError(error).message;
    } finally {
      isResetting.value = false;
    }
  };

  return {
    actionErrorMessage, address, completedCount, fetchChecklist, isLoading,
    isResetting, items, loadErrorMessage, pendingItemIds, progress,
    resetItems, toggleItem,
  };
};
