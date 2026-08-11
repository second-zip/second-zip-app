<script setup>
import { computed } from 'vue';

import ClearIcon from '@/assets/icons/report/cancel-gray-18.svg';

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['update:modelValue']);

const quickAmounts = [10, 100, 1000, 10000];

const hasDeposit = computed(() => props.modelValue.length > 0);

const updateDeposit = (event) => {
  const value = event.target.value.replace(/\D/g, '');
  emit('update:modelValue', value);
};

const addAmount = (amount) => {
  const currentValue = Number(props.modelValue) || 0;
  emit('update:modelValue', String(currentValue + amount));
};

const clearDeposit = () => {
  emit('update:modelValue', '');
};
</script>

<template>
  <section class="deposit-form-box">
    <h3 class="form-section-title fw-bold mb-3">
      <span>보증금</span>을 입력해 주세요
    </h3>

    <div class="deposit-input-row d-flex align-items-center">
      <div class="deposit-input-box d-flex align-items-center flex-grow-1">
        <input
          :value="modelValue"
          type="text"
          inputmode="numeric"
          class="deposit-input flex-grow-1 border-0 fw-semibold"
          placeholder="0"
          aria-label="보증금"
          @input="updateDeposit"
        />

        <button
          v-if="hasDeposit"
          type="button"
          class="deposit-clear-button d-flex align-items-center justify-content-center border-0 p-0"
          aria-label="보증금 지우기"
          @click="clearDeposit"
        >
          <img :src="ClearIcon" alt="" />
        </button>
      </div>

      <span class="deposit-unit fw-bold">만원</span>
    </div>

    <div class="quick-amounts d-grid">
      <button
        v-for="amount in quickAmounts"
        :key="amount"
        type="button"
        class="quick-amount-button border-0 fw-medium"
        @click="addAmount(amount)"
      >
        {{ amount }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.deposit-form-box {
  padding: 24px 0;
}

.form-section-title {
  color: var(--black-900);
  font-size: 1.25rem;
  line-height: 1.4;
  letter-spacing: -0.04em;
}

.form-section-title span {
  color: var(--blue-900);
}

.deposit-input-row {
  gap: 10px;
}

.deposit-input-box {
  min-width: 0;
  height: 54px;
  padding: 0 14px 0 16px;
  background-color: #fff;
  border: 1px solid var(--black-100);
  border-radius: 16px;
  transition: border-color 0.15s ease;
}

.deposit-input-box:focus-within {
  border-color: var(--blue-700);
}

.deposit-input {
  min-width: 0;
  color: var(--black-900);
  font-size: 1rem;
  background-color: transparent;
  outline: none;
}

.deposit-input::placeholder {
  color: var(--black-300);
}

.deposit-clear-button {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  background-color: transparent;
}

.deposit-clear-button img {
  width: 20px;
  height: 20px;
}

.deposit-unit {
  flex-shrink: 0;
  color: var(--black-900);
  font-size: 1.25rem;
}

.quick-amounts {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.quick-amount-button {
  height: 40px;
  color: #fff;
  font-size: 0.8125rem;
  background-color: var(--blue-700);
  border-radius: 12px;
}

.quick-amount-button:active {
  transform: translateY(1px);
}

.quick-amount-button:focus-visible {
  outline: 2px solid var(--blue-900);
  outline-offset: 2px;
}
</style>
