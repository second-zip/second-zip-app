<script setup>
import ContentBox from '@/components/common/ContentBox.vue';

defineProps({
  title: { type: String, required: true },
  items: { type: Array, default: () => [] },
  itemKey: { type: String, default: 'id' },
  isLoading: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
  loadingMessage: { type: String, default: '목록을 불러오는 중입니다.' },
  emptyMessage: { type: String, default: '목록이 없습니다.' },
});
</script>

<template>
  <div class="list-box w-100 d-flex flex-column">
    <div class="list-box__title d-flex align-items-center">
      <h3 class="fw-bold fs-6">{{ title }}</h3>
      <span class="fw-semibold">{{ items.length }}건</span>
    </div>
    <ContentBox flush>
      <p v-if="isLoading" class="list-box__feedback mb-0" role="status">
        {{ loadingMessage }}
      </p>
      <p
        v-else-if="errorMessage"
        class="list-box__feedback list-box__feedback--error mb-0"
        role="alert"
      >{{ errorMessage }}</p>
      <p v-else-if="items.length === 0" class="list-box__feedback mb-0">
        {{ emptyMessage }}
      </p>
      <template v-else>
        <template
          v-for="(item, index) in items"
          :key="item[itemKey] ?? index"
        >
          <slot name="item" :item="item" :index="index" />
        </template>
      </template>
    </ContentBox>
  </div>
</template>

<style scoped>
.list-box { height: fit-content; gap: 8px; }
.list-box__title { height: fit-content; gap: 8px; }
.list-box__title h3 {
  height: fit-content;
  margin: 0;
  color: var(--black-900);
}

.list-box__title span {
  padding: 3px 10px;
  color: var(--black-500);
  background-color: var(--black-100);
  border-radius: 10px;
  font-size: 0.75rem;
}

.list-box__feedback {
  padding: 28px 16px;
  color: var(--black-500);
  font-size: 0.875rem;
  text-align: center;
}

.list-box__feedback--error { color: var(--red-500); }
</style>
