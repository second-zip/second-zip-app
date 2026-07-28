<script setup>
import dictIcon from '@/assets/icons/nav/dict-blue-22.svg';

defineProps({
  title: {
    type: String,
    required: true,
  },
});
</script>

<template>
  <div
    class="container w-100 h-100 d-flex align-items-center justify-content-between"
  >
    <RouterLink
      to="/dict"
      class="dict-title d-flex align-items-center gap-2 text-decoration-none"
    >
      <span class="fs-4 fw-bold text-nowrap"> 전세사기 도감 </span>

      <img :src="dictIcon" class="dict-icon flex-shrink-0" alt="" />
    </RouterLink>

    <h1 class="current-title fw-normal text-end mb-0">
      {{ title }}
    </h1>
  </div>
</template>

<style scoped>
.container {
  padding: 0 20px;
}

.dict-title {
  color: var(--black-900);
}

.dict-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.current-title {
  min-width: 0;
  color: var(--black-700);
  font-size: 0.875rem;
  white-space: nowrap;
}
</style>
