<script setup>
import { computed } from 'vue';

import {
  REPORT_STATUS_FALLBACK,
  REPORT_STATUS_MAP,
} from '@/constants/report/list';

const props = defineProps({
  result: { type: String, default: '' },
});
const status = computed(
  () => REPORT_STATUS_MAP[props.result] ?? REPORT_STATUS_FALLBACK,
);
</script>

<template>
  <div
    class="status d-flex flex-shrink-0 align-items-center justify-content-center"
    :class="`status--${status.className}`"
  >
    <img :src="status.icon" class="status__icon" :alt="status.alt" />
  </div>
</template>

<style scoped>
.status {
  width: 36px;
  height: 36px;
  border-radius: 14px;
}

.status--safe,
.status--unknown {
  background-color: var(--green-100);
}

.status--caution {
  background-color: var(--yellow-100);
}

.status--danger {
  background-color: var(--red-100);
}

.status__icon {
  width: 22px;
  height: 22px;
  object-fit: contain;
}
</style>
