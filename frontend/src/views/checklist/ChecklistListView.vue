<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';

import ChecklistListContent from '@/components/checklist/ChecklistListContent.vue';
import MemberSecretaryGuide from '@/components/common/secretary/MemberSecretaryGuide.vue';
import { useChecklistList } from '@/composables/checklist/useChecklistList';
import { CHECKLIST_LIST_GUIDE_MESSAGES } from '@/constants/checklist/guide';
import BottomSheetLayout from '@/layouts/BottomSheetLayout.vue';
import DefaultSheetHeader from '@/layouts/DefaultSheetHeader.vue';

const router = useRouter();
const {
  checklists,
  creatingReportIds,
  isLoading,
  errorMessage,
  creationErrorMessage,
  fetchChecklists,
  createChecklist,
} = useChecklistList();
const handleCreateChecklist = async (report) => {
  const createdChecklist = await createChecklist(report);

  if (!createdChecklist?.reportChecklistId) return;

  await router.push({
    name: 'checklist-detail',
    params: { reportChecklistId: createdChecklist.reportChecklistId },
  });
};

onMounted(fetchChecklists);
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
      <ChecklistListContent
        :checklists="checklists"
        :creating-report-ids="creatingReportIds"
        :is-loading="isLoading"
        :error-message="errorMessage"
        :creation-error-message="creationErrorMessage"
        @create="handleCreateChecklist"
      />
    </div>
  </BottomSheetLayout>

  <MemberSecretaryGuide
    :messages="CHECKLIST_LIST_GUIDE_MESSAGES"
    floating
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

</style>
