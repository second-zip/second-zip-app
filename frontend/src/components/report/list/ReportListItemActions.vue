<script setup>
import { computed, ref } from 'vue';

import {
  REPORT_ACTION_ICONS,
  REPORT_DETAIL_ROUTE_NAME,
} from '@/constants/report/list';

const props = defineProps({
  report: { type: Object, required: true },
  address: { type: String, required: true },
});
const emit = defineEmits(['toggle-favorite', 'delete']);
const isDeleteHovered = ref(false);
const detailRoute = computed(() => ({
  name: REPORT_DETAIL_ROUTE_NAME,
  params: { analysisReportId: props.report.analysisReportId },
}));
</script>

<template>
  <div class="actions d-flex flex-shrink-0 align-items-center">
    <button
      type="button"
      class="action-button d-flex align-items-center justify-content-center border-0 p-0"
      :aria-pressed="Boolean(report.favorite)"
      :aria-label="report.favorite ? '즐겨찾기 해제' : '즐겨찾기 추가'"
      @click.stop="emit('toggle-favorite', report)"
    >
      <img
        :src="
          report.favorite
            ? REPORT_ACTION_ICONS.favoriteActive
            : REPORT_ACTION_ICONS.favorite
        "
        class="action-icon"
        alt=""
      />
    </button>
    <button
      type="button"
      class="action-button d-flex align-items-center justify-content-center border-0 p-0"
      aria-label="리포트 삭제"
      @click.stop="emit('delete', report)"
      @mouseenter="isDeleteHovered = true"
      @mouseleave="isDeleteHovered = false"
      @focus="isDeleteHovered = true"
      @blur="isDeleteHovered = false"
    >
      <img
        :src="
          isDeleteHovered
            ? REPORT_ACTION_ICONS.deleteActive
            : REPORT_ACTION_ICONS.delete
        "
        class="action-icon"
        alt=""
      />
    </button>
    <RouterLink
      :to="detailRoute"
      class="detail-button d-flex align-items-center justify-content-center p-0"
      :aria-label="`${address} 리포트 상세 보기`"
    >
      <img :src="REPORT_ACTION_ICONS.detail" class="arrow-icon" alt="" />
    </RouterLink>
  </div>
</template>

<style scoped>
.actions {
  gap: 4px;
}

.action-button,
.detail-button {
  width: 24px;
  height: 24px;
  background-color: transparent;
}

.action-icon {
  width: 24px;
  height: 24px;
}

.arrow-icon {
  width: 14px;
  height: 14px;
}

.detail-button:focus-visible {
  border-radius: 4px;
  outline: 2px solid var(--blue-700);
  outline-offset: 2px;
}

@media (max-width: 360px) {
  .actions {
    gap: 2px;
  }
}
</style>
