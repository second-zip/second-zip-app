<!-- 보증금과 전세가율을 보여주는 두 번째 분석 구역입니다. -->
<script setup>
import ContentBox from '@/components/common/ContentBox.vue';

defineProps({
  deposit: { type: String, required: true },
  ratio: { type: String, required: true },
});
</script>

<template>
  <ContentBox class="price-card">
    <div class="price-field">
      <span>입력한 보증금</span>
      <strong class="deposit-value">{{ deposit }}</strong>
    </div>
    <div class="price-divider" aria-hidden="true"></div>
    <div class="ratio-field">
      <span>전세가율 <small>(80% 미만 안전)</small></span>
      <strong>{{ ratio }}</strong>
    </div>
  </ContentBox>
</template>

<style scoped>
.price-card {
  display: grid !important;
  grid-template-columns: minmax(0, 1fr) 0.0625rem minmax(0, 1fr);
  align-items: center;
  min-height: 5.875rem;
  margin-top: 1rem;
  padding: 0.75rem 1.125rem;
  background: var(--blue-100);
  border-color: var(--blue-300) !important;
  border-radius: 1rem !important;
}

.price-field,
.ratio-field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
}

.price-field {
  padding-right: 1rem;
}

.ratio-field {
  padding-left: 1.25rem;
}

.price-field span,
.ratio-field span {
  color: var(--blue-900);
  font-size: 0.75rem;
  font-weight: 700;
}

.ratio-field small {
  color: var(--blue-500);
  font-size: 0.5625rem;
  white-space: nowrap;
}

.deposit-value,
.ratio-field strong {
  margin-top: 0.25rem;
  color: var(--black-900);
  font-size: 1.125rem;
  font-weight: 600;
  line-height: 1.25;
}

.deposit-value {
  min-width: 0;
  white-space: nowrap;
}

.price-divider {
  width: 0.0625rem;
  height: 4.25rem;
  background: var(--blue-300);
}
</style>
