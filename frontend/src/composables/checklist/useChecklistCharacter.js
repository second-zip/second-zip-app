import { computed, onMounted, ref } from 'vue';

import { REPORT_CHARACTER_TYPES } from '@/constants/report/list';
import { useAuthStore } from '@/stores/auth';
import { logger } from '@/utils/logger';

export const useChecklistCharacter = (logScope) => {
  const authStore = useAuthStore();
  const isCharacterLoading = ref(
    authStore.isAuthenticated && !authStore.myPage?.characterType,
  );
  const characterType = computed(() => {
    const type = authStore.myPage?.characterType ?? authStore.characterType;
    return REPORT_CHARACTER_TYPES.has(type) ? type : 'CAT';
  });

  onMounted(async () => {
    if (!isCharacterLoading.value) return;
    try {
      await authStore.fetchMyPage();
    } catch (error) {
      logger.error(`${logScope}.fetch-user`, error);
    } finally {
      isCharacterLoading.value = false;
    }
  });

  return { characterType, isCharacterLoading };
};
