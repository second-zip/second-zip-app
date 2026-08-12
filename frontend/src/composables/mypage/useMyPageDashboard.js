import { computed, onMounted, ref } from 'vue';

import { getReports } from '@/api/report';
import { getApiError } from '@/api/utils/error';
import { SECRETARY_OPTIONS } from '@/constants/mypage';
import { useAuthStore } from '@/stores/auth';
import { summarizeReports } from '@/utils/mypage';

export const useMyPageDashboard = () => {
  const authStore = useAuthStore();
  const errorMessage = ref('');
  const activityLoading = ref(false);
  const activity = ref(summarizeReports());

  const account = computed(() => authStore.myPage ?? {});
  const secretaryLabel = computed(() => {
    const option = SECRETARY_OPTIONS.find(
      ({ value }) => value === authStore.characterType,
    );
    return option?.label ?? 'AI 비서';
  });
  const secretaryImage = computed(() => {
    const option = SECRETARY_OPTIONS.find(
      ({ value }) => value === authStore.characterType,
    );
    return option?.mainImage ?? SECRETARY_OPTIONS[0].mainImage;
  });

  const loadDashboard = async () => {
    activityLoading.value = true;
    const [accountResult, reportsResult] = await Promise.allSettled([
      authStore.fetchMyPage(),
      getReports(),
    ]);

    if (accountResult.status === 'rejected') {
      errorMessage.value = getApiError(accountResult.reason).message;
    }
    if (reportsResult.status === 'fulfilled') {
      // GET /analysis-reports의 totalCount와 각 report.result를 화면 집계값으로 변환합니다.
      activity.value = summarizeReports(reportsResult.value);
    } else {
      errorMessage.value ||= getApiError(reportsResult.reason).message;
    }
    activityLoading.value = false;
  };

  onMounted(loadDashboard);

  return {
    account,
    activity,
    activityLoading,
    errorMessage,
    secretaryImage,
    secretaryLabel,
  };
};
