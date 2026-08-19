<!-- 필수 점검 결과를 펼쳐보는 세 번째 분석 구역입니다. -->
<script setup>
import { ref } from 'vue';

import ContentBox from '@/components/common/ContentBox.vue';

defineProps({
  checks: { type: Array, required: true },
  risk: { type: String, required: true },
  icons: { type: Object, required: true },
  labels: { type: Object, required: true },
});

const openedItem = ref('');
const toggle = (id) => {
  openedItem.value = openedItem.value === id ? '' : id;
};
</script>

<template>
  <ContentBox class="inspection-card" shadow>
    <div class="inspection-heading d-flex align-items-center">
      <span class="heading-dot" aria-hidden="true"></span>
      <h2 class="mb-0">필수 점검</h2>
      <img :src="icons[risk]" :alt="risk" />
      <span class="status-pill" :class="`status-pill--${risk}`">
        {{ labels[risk] }}
      </span>
    </div>

    <div class="accordion accordion-flush">
      <article v-for="check in checks" :key="check.id" class="accordion-item">
        <h3 class="accordion-header">
          <button
            class="accordion-button d-flex"
            :class="{ collapsed: openedItem !== check.id }"
            type="button"
            :aria-expanded="openedItem === check.id"
            :aria-controls="`${check.id}-detail`"
            @click="toggle(check.id)"
          >
            <img :src="icons[check.status]" alt="" />
            <span>{{ check.label }}</span>
          </button>
        </h3>

        <div
          :id="`${check.id}-detail`"
          class="accordion-collapse collapse"
          :class="{ show: openedItem === check.id }"
        >
          <div
            class="accordion-body"
            :class="`accordion-body--${check.status}`"
          >
            <div class="detail-row">
              <span>판정 근거</span>
              <strong>{{ check.basis }}</strong>
            </div>
            <div class="detail-row">
              <span>우리 집은?</span>
              <strong :class="`text-${check.status}`">{{
                check.amount
              }}</strong>
            </div>
          </div>
        </div>
      </article>
    </div>
  </ContentBox>
</template>

<style scoped>
.inspection-card {
  margin-top: 1rem;
  overflow: hidden;
  border-color: var(--black-100) !important;
  border-radius: 1rem !important;
}

.inspection-heading {
  min-height: 3.25rem;
  padding: 0 1rem;
  gap: 0.5rem;
  border-bottom: 0.0625rem solid var(--black-100);
}

.inspection-heading h2 {
  flex: 1;
  font-size: 0.875rem;
  font-weight: 700;
}

.inspection-heading img {
  width: 1.125rem;
  height: 1.125rem;
}

.heading-dot {
  width: 0.375rem;
  height: 0.375rem;
  background: var(--blue-900);
  border-radius: 50%;
}

.status-pill {
  padding: 0.125rem 0.375rem;
  border-radius: 999rem;
  font-size: 0.5625rem;
  font-weight: 700;
}

.status-pill--safe {
  color: var(--green-500);
  background: var(--green-100);
}

.status-pill--caution {
  color: var(--yellow-500);
  background: var(--yellow-100);
}

.status-pill--danger {
  color: var(--red-500);
  background: var(--red-100);
}

.accordion-item {
  border-color: var(--black-100);
}

.accordion-button {
  min-height: 3rem;
  padding: 0.75rem 1rem;
  gap: 0.625rem;
  color: var(--black-900);
  background: white;
  font-size: 0.75rem;
  font-weight: 500;
  box-shadow: none;
}

.accordion-button:not(.collapsed) {
  color: var(--black-900);
  background: white;
  box-shadow: none;
}

.accordion-button:focus {
  box-shadow: none;
}

.accordion-button img {
  width: 1.125rem;
  height: 1.125rem;
  flex: 0 0 1.125rem;
}

.accordion-button::after {
  width: 0.75rem;
  height: 0.75rem;
  margin-left: auto;
  background-size: 0.75rem;
  opacity: 0.55;
}

.accordion-body {
  margin: 0 0.875rem 0.875rem;
  padding: 0.25rem 0.75rem;
  background: var(--green-100);
  border: 0.0625rem solid var(--green-500);
  border-radius: 0.75rem;
}

.accordion-body--caution {
  background: var(--yellow-100);
  border-color: var(--yellow-500);
}

.accordion-body--danger {
  background: var(--red-100);
  border-color: var(--red-500);
}

.detail-row {
  display: grid;
  grid-template-columns: 4.5rem minmax(0, 1fr);
  padding: 0.625rem 0;
  font-size: 0.6875rem;
}

.detail-row + .detail-row {
  border-top: 0.0625rem solid rgb(18 183 106 / 30%);
}

.accordion-body--caution .detail-row + .detail-row {
  border-color: rgb(247 144 9 / 30%);
}

.accordion-body--danger .detail-row + .detail-row {
  border-color: rgb(247 9 9 / 30%);
}

.detail-row span {
  font-weight: 600;
}

.detail-row strong {
  font-weight: 500;
}

.text-safe {
  color: var(--green-500);
}

.text-caution {
  color: var(--yellow-500);
}

.text-danger {
  color: var(--red-500) !important;
}
</style>
