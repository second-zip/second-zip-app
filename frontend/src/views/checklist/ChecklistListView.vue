<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import ChecklistReportItem from '@/components/checklist/ChecklistReportItem.vue';
import ListBox from '@/components/common/ListBox.vue';
import SecretaryGuide from '@/components/common/secretary/SecretaryGuide.vue';
import { useChecklistList } from '@/composables/checklist/useChecklistList';
import { REPORT_CHARACTER_TYPES } from '@/constants/report/list';
import BottomSheetLayout from '@/layouts/BottomSheetLayout.vue';
import DefaultSheetHeader from '@/layouts/DefaultSheetHeader.vue';
import { useAuthStore } from '@/stores/auth';
import { logger } from '@/utils/logger';

const authStore = useAuthStore();
const router = useRouter();
const {
  checklists,
  isLoading,
  errorMessage,
  creationErrorMessage,
  fetchChecklists,
  createChecklist,
  isCreatingChecklist,
} = useChecklistList();
const isCharacterLoading = ref(
  authStore.isAuthenticated && !authStore.myPage?.characterType,
);
const characterType = computed(() => {
  const type = authStore.myPage?.characterType ?? authStore.characterType;

  return REPORT_CHARACTER_TYPES.has(type) ? type : 'CAT';
});

const handleCreateChecklist = async (report) => {
  const createdChecklist = await createChecklist(report);

  if (!createdChecklist?.reportChecklistId) return;

  await router.push({
    name: 'checklist-detail',
    params: { reportChecklistId: createdChecklist.reportChecklistId },
  });
};

onMounted(fetchChecklists);
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
        <ListBox
          title="리포트 목록"
          :items="checklists"
          item-key="analysisReportId"
          :is-loading="isLoading"
          :error-message="errorMessage"
          loading-message="체크리스트 목록을 불러오는 중입니다."
          empty-message="체크리스트를 만들 리포트가 없습니다."
        >
          <template #item="{ item }">
            <ChecklistReportItem
              :report="item"
              :is-creating="isCreatingChecklist(item.analysisReportId)"
              @create="handleCreateChecklist"
            />
          </template>
        </ListBox>
        <p
          v-if="creationErrorMessage"
          class="checklist-list__error mt-2 mb-0 text-center"
          role="alert"
        >{{ creationErrorMessage }}</p>
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

.checklist-list__error {
  color: var(--red-500);
  font-size: 0.75rem;
}
</style>
