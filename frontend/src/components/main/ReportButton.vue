<script setup>
import BaseButton from '@/components/common/BaseButton.vue';
import ReportIcon from '@/assets/icons/main/protect-white-18.svg';

const emit = defineEmits(['click']);
</script>

<template>
  <BaseButton type="button" class="analyze-button" @click="emit('click')">
    <span class="d-flex align-items-center justify-content-center gap-2">
      <img :src="ReportIcon" />
      <span>분석하기</span>
    </span>
  </BaseButton>
</template>

<style scoped>
.analyze-button {
  height: 56px;
  font-size: 1.125rem;
}

.analyze-button__icon {
  width: 22px;
  height: 22px;
}
</style>
