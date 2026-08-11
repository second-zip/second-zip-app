<script setup>
import { computed, onMounted, ref } from 'vue';

import ChecklistDescriptionModal from '@/components/checklist/ChecklistDescriptionModal.vue';
import ChecklistDetailHeader from '@/components/checklist/ChecklistDetailHeader.vue';
import ChecklistItem from '@/components/checklist/ChecklistItem.vue';
import ChecklistProgress from '@/components/checklist/ChecklistProgress.vue';
import ChecklistRecorder from '@/components/checklist/ChecklistRecorder.vue';
import SecretaryGuide from '@/components/common/secretary/SecretaryGuide.vue';
import { REPORT_CHARACTER_TYPES } from '@/constants/report/list';
import { useAuthStore } from '@/stores/auth';
import { logger } from '@/utils/logger';

const CHECKLIST_ADDRESS = '서울시 마포구 합정동 123-45';

const authStore = useAuthStore();
const checklistItems = ref([
  {
    id: 1,
    title: '선순위 임차인 보증금',
    description:
      '선순위 임차인의 보증금 규모를 확인해 보세요. 보증금 반환 가능성을 판단하는 데 중요한 항목이에요.',
    checked: false,
  },
  {
    id: 2,
    title: '확정일자 부여현황',
    description:
      '확정일자 부여 현황을 통해 기존 임차인의 권리 관계를 확인할 수 있어요.',
    checked: true,
  },
  {
    id: 3,
    title: '전입세대확인서',
    description:
      '전입세대확인서를 통해 해당 주택에 전입한 세대 정보를 확인해 보세요.',
    checked: false,
  },
  {
    id: 4,
    title: '등기부등본 확인',
    description:
      '등기부등본에서 소유권과 근저당권 등 권리 관계를 확인해 보세요.',
    checked: false,
  },
  {
    id: 5,
    title: '건축물대장 확인',
    description:
      '건축물대장을 통해 건물의 용도와 위반건축물 여부 등을 확인해 보세요.',
    checked: false,
  },
  {
    id: 6,
    title: '전세가율 확인',
    description:
      '매매가 대비 전세보증금 비율을 확인해 보세요. 전세가율이 높을수록 주의가 필요할 수 있어요.',
    checked: false,
  },
]);
const selectedItem = ref(null);
const isDescriptionModalOpen = ref(false);
const isRecorderModalOpen = ref(false);
const isCharacterLoading = ref(
  authStore.isAuthenticated && !authStore.myPage?.characterType,
);

const completedCount = computed(
  () => checklistItems.value.filter((item) => item.checked).length,
);
const totalCount = computed(() => checklistItems.value.length);
const progress = computed(() =>
  totalCount.value ? (completedCount.value / totalCount.value) * 100 : 0,
);
const characterType = computed(() => {
  const type = authStore.myPage?.characterType ?? authStore.characterType;

  return REPORT_CHARACTER_TYPES.has(type) ? type : 'CAT';
});

const toggleItem = (id) => {
  const item = checklistItems.value.find((item) => item.id === id);

  if (item) item.checked = !item.checked;
};

const resetChecklist = () => {
  checklistItems.value.forEach((item) => {
    item.checked = false;
  });
};

const openDescription = (item) => {
  selectedItem.value = item;
  isDescriptionModalOpen.value = true;
};

const closeDescription = () => {
  isDescriptionModalOpen.value = false;
  selectedItem.value = null;
};

onMounted(async () => {
  if (!isCharacterLoading.value) return;

  try {
    await authStore.fetchMyPage();
  } catch (error) {
    logger.error('checklist-detail.fetch-user', error);
  } finally {
    isCharacterLoading.value = false;
  }
});
</script>

<template>
  <div class="checklist-detail-view w-100 h-100 overflow-y-auto">
    <ChecklistDetailHeader
      :address="CHECKLIST_ADDRESS"
      @reset="resetChecklist"
    />

    <ChecklistRecorder
      class="mt-3"
      @modal-visibility-change="isRecorderModalOpen = $event"
    />

    <ChecklistProgress
      class="mt-4"
      :completed-count="completedCount"
      :total-count="totalCount"
      :progress="progress"
    />

    <section
      class="checklist-items d-flex flex-column mt-4"
      aria-label="계약 체크리스트"
    >
      <ChecklistItem
        v-for="item in checklistItems"
        :key="item.id"
        :item="item"
        @toggle="toggleItem(item.id)"
        @show-description="openDescription(item)"
      />
    </section>
  </div>

  <SecretaryGuide
    v-if="
      !isCharacterLoading && !isDescriptionModalOpen && !isRecorderModalOpen
    "
    :floating="true"
    :character-type="characterType"
    text="체크리스트 상단이 주택유형에 따른
    특약이니 중요도가 높다냐-오옹"
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

.checklist-items {
  gap: 12px;
}
</style>
