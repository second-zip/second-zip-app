<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import ChecklistDescriptionModal from '@/components/checklist/ChecklistDescriptionModal.vue';
import ChecklistDetailHeader from '@/components/checklist/ChecklistDetailHeader.vue';
import ChecklistDetailList from '@/components/checklist/ChecklistDetailList.vue';
import ChecklistProgress from '@/components/checklist/ChecklistProgress.vue';
import ChecklistRecorder from '@/components/checklist/ChecklistRecorder.vue';
import SecretaryGuide from '@/components/common/secretary/SecretaryGuide.vue';
import { useChecklistCharacter } from '@/composables/checklist/useChecklistCharacter';
import { useChecklistDetail } from '@/composables/checklist/useChecklistDetail';

const route = useRoute();
const reportChecklistId = computed(
  () => Number(route.params.reportChecklistId),
);
const selectedItem = ref(null);
const isDescriptionModalOpen = ref(false);
const isRecorderModalOpen = ref(false);
const { characterType, isCharacterLoading } =
  useChecklistCharacter('checklist-detail');
const detail = useChecklistDetail(reportChecklistId);

const openDescription = (item) => {
  selectedItem.value = item;
  isDescriptionModalOpen.value = true;
};
const closeDescription = () => {
  isDescriptionModalOpen.value = false;
  selectedItem.value = null;
};

onMounted(detail.fetchChecklist);
</script>

<template>
  <div class="checklist-detail-view w-100 h-100 overflow-y-auto">
    <ChecklistDetailHeader
      :address="detail.address.value"
      :is-resetting="detail.isResetting.value"
      @reset="detail.resetItems"
    />
    <ChecklistRecorder
      class="mt-3"
      @modal-visibility-change="isRecorderModalOpen = $event"
    />
    <ChecklistProgress
      class="mt-4"
      :completed-count="detail.completedCount.value"
      :total-count="detail.items.value.length"
      :progress="detail.progress.value"
    />
    <ChecklistDetailList
      :items="detail.items.value"
      :is-loading="detail.isLoading.value"
      :load-error-message="detail.loadErrorMessage.value"
      :action-error-message="detail.actionErrorMessage.value"
      @toggle="detail.toggleItem"
      @show-description="openDescription"
    />
  </div>
  <SecretaryGuide
    v-if="!isCharacterLoading && !isDescriptionModalOpen && !isRecorderModalOpen"
    floating
    :character-type="characterType"
    text="체크리스트를 하나씩 확인해 보자냥!"
  />
  <ChecklistDescriptionModal
    :item="selectedItem"
    :open="isDescriptionModalOpen"
    @close="closeDescription"
  />
</template>

<style scoped>
.checklist-detail-view {
  padding: 20px 20px 112px;
  overscroll-behavior: contain;
}
</style>
