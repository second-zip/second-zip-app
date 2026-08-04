<!-- 전세사기 유형별 판정을 펼쳐보는 네 번째 분석 구역입니다. -->
<script setup>
import { ref } from 'vue';

import ContentBox from '@/components/common/ContentBox.vue';
import { getAggregateRiskStatus } from '@/utils/report/analysis';

defineProps({
  fraudTypes: { type: Array, required: true },
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
  <ContentBox class="fraud-card" shadow>
    <div class="fraud-heading d-flex align-items-center">
      <span class="heading-dot" aria-hidden="true"></span>
      <h2 class="mb-0">예방 가능한 전세사기 유형</h2>
      <img :src="icons[risk]" :alt="risk" />
      <span class="status-pill" :class="`status-pill--${risk}`">
        {{ labels[risk] }}
      </span>
    </div>

    <div class="accordion accordion-flush">
      <article
        v-for="fraudType in fraudTypes"
        :key="fraudType.id"
        class="accordion-item fraud-type"
      >
        <h3 class="accordion-header">
          <button
            class="accordion-button fraud-type-button"
            :class="{ collapsed: openedItem !== fraudType.id }"
            type="button"
            :aria-expanded="openedItem === fraudType.id"
            :aria-controls="`${fraudType.id}-detail`"
            @click="toggle(fraudType.id)"
          >
            <img
              class="fraud-type-status"
              :src="icons[getAggregateRiskStatus(fraudType.items)]"
              alt=""
            />
            <span class="fraud-type-copy">
              <strong>{{ fraudType.title }}</strong>
              <small>{{ fraudType.subtitle }}</small>
            </span>
          </button>
        </h3>

        <div
          :id="`${fraudType.id}-detail`"
          class="accordion-collapse collapse"
          :class="{ show: openedItem === fraudType.id }"
        >
          <div class="fraud-items">
            <div
              v-for="item in fraudType.items"
              :key="item.label"
              class="fraud-item d-flex align-items-center"
              :class="`fraud-item--${item.status}`"
            >
              <span>{{ item.label }}</span>
              <img :src="icons[item.status]" :alt="item.status" />
            </div>
          </div>
        </div>
      </article>
    </div>
  </ContentBox>
</template>

<style scoped>
.fraud-card {
  margin-top: 1rem;
  overflow: hidden;
  border-color: var(--black-100) !important;
  border-radius: 1rem !important;
}

.fraud-heading {
  min-height: 3.25rem;
  padding: 0 1rem;
  gap: 0.5rem;
  border-bottom: 0.0625rem solid var(--black-100);
}

.fraud-heading h2 {
  flex: 1;
  font-size: 0.875rem;
  font-weight: 700;
}

.fraud-heading img,
.fraud-type-status {
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

.accordion-button::after {
  width: 0.75rem;
  height: 0.75rem;
  margin-left: auto;
  background-size: 0.75rem;
  opacity: 0.55;
}

.fraud-type-button {
  min-height: 3.75rem;
}

.fraud-type-status {
  flex: 0 0 1.125rem;
}

.fraud-type-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 0.1875rem;
}

.fraud-type-copy strong {
  font-size: 0.75rem;
  font-weight: 700;
}

.fraud-type-copy small {
  overflow: hidden;
  color: var(--black-300);
  font-size: 0.625rem;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fraud-items {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0 0.875rem 0.875rem;
}

.fraud-item {
  min-height: 2.5rem;
  padding: 0.5rem 0.75rem;
  gap: 0.625rem;
  background: var(--green-100);
  border: 0.0625rem solid var(--green-500);
  border-radius: 0.625rem;
  font-size: 0.6875rem;
  font-weight: 600;
}

.fraud-item span {
  min-width: 0;
  flex: 1;
}

.fraud-item img {
  width: 1.125rem;
  height: 1.125rem;
}

.fraud-item--caution {
  background: var(--yellow-100);
  border-color: var(--yellow-500);
}

.fraud-item--danger {
  background: var(--red-100);
  border-color: var(--red-500);
}
</style>
