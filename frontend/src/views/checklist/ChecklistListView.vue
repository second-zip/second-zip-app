<script setup>
import { computed, onMounted, ref } from 'vue';

import ChecklistReportItem from '@/components/checklist/ChecklistReportItem.vue';
import SecretaryGuide from '@/components/common/secretary/SecretaryGuide.vue';
import ReportListBox from '@/components/report/list/ReportListBox.vue';
import { useReportList } from '@/composables/report/useReportList';
import { REPORT_CHARACTER_TYPES } from '@/constants/report/list';
import BottomSheetLayout from '@/layouts/BottomSheetLayout.vue';
import DefaultSheetHeader from '@/layouts/DefaultSheetHeader.vue';
import { useAuthStore } from '@/stores/auth';
import { logger } from '@/utils/logger';

const authStore = useAuthStore();
const { reports, isLoading, errorMessage, fetchReports } = useReportList();
const isCharacterLoading = ref(
  authStore.isAuthenticated && !authStore.myPage?.characterType,
);
const characterType = computed(() => {
  const type = authStore.myPage?.characterType ?? authStore.characterType;

  return REPORT_CHARACTER_TYPES.has(type) ? type : 'CAT';
});

onMounted(fetchReports);
onMounted(async () => {
  if (!isCharacterLoading.value) return;

  try {
    await authStore.fetchMyPage();
  } catch (error) {
    logger.error('checklist-list.fetch-user', error);
  } finally {
    isCharacterLoading.value = false;
  }
});
</script>

<template>
  <BottomSheetLayout :title-ratio="15">
    <template #header>
      <div class="checklist-header w-100 h-100 d-flex flex-column px-3 py-3">
        <DefaultSheetHeader
          class="checklist-header__title flex-grow-1"
          title="체크리스트"
        />
      </div>
    </template>

    <div class="checklist-sheet-scroll w-100 h-100 overflow-y-auto">
      <section class="checklist-list w-100">
        <ReportListBox
          :reports="reports"
          :is-loading="isLoading"
          :error-message="errorMessage"
        >
          <template #item="{ report, index }">
            <ChecklistReportItem :report="report" :index="index" />
          </template>
        </ReportListBox>
      </section>
    </div>
  </BottomSheetLayout>

  <SecretaryGuide
    v-if="!isCharacterLoading"
    :floating="true"
    :character-type="characterType"
    text="각 리포트별로 체크리스트를 만들 수 있다냥! 안전한 계약을 이번집이 도와주겠다냥~"
  />
</template>

<style scoped>
.checklist-header__eyebrow {
  color: var(--black-300);
  font-size: 0.75rem;
}

.checklist-header__title {
  min-height: 0;
}

.checklist-sheet-scroll {
  border-radius: 36px 36px 0 0;
  overscroll-behavior: contain;
}

.checklist-list {
  min-height: 100%;
  padding: 24px 20px;
}
</style>
