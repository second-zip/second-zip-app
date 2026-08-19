<script setup>
import HomeIcon from '@/assets/icons/main/home-red-20.svg';
import GraphIcon from '@/assets/icons/main/graph-green-20.svg';

const props = defineProps({
  modelValue: {
    type: String,
    default: 'fraud-damage',
  },
});

const emit = defineEmits(['update:modelValue']);

const selectTab = (value) => {
  emit('update:modelValue', value);
};
</script>

<template>
  <div class="data-tabs w-100 d-flex">
    <button
      type="button"
      class="data-tabs__button flex-fill d-flex align-items-center justify-content-center gap-2 border-0"
      :class="{
        'data-tabs__button--damage': props.modelValue === 'fraud-damage',
      }"
      @click="selectTab('fraud-damage')"
    >
      <img
        :src="HomeIcon"
        alt=""
        class="data-tabs__icon"
        :class="
          props.modelValue === 'fraud-damage' ? 'opacity-100' : 'opacity-50'
        "
      />
      <span class="data_tabs__label fw-semibold">전세사기 피해주택</span>
    </button>

    <button
      type="button"
      class="data-tabs__button flex-fill d-flex align-items-center justify-content-center gap-2 border-0"
      :class="{
        'data-tabs__button--price': props.modelValue === 'price-index',
      }"
      @click="selectTab('price-index')"
    >
      <img
        :src="GraphIcon"
        alt=""
        class="data-tabs__icon"
        :class="
          props.modelValue === 'price-index' ? 'opacity-100' : 'opacity-50'
        "
      />
      <span class="data_tabs__label fw-semibold">전세가격 지수변동</span>
    </button>
  </div>
</template>

<style scoped>
.data-tabs {
  height: 42px;
  padding: 4px;
  overflow: hidden;
  background-color: #fff;
  border: 1px solid var(--black-100);
  border-radius: 14px;
  box-shadow: 0 2px 8px rgb(17 17 24 / 6%);
}

.data-tabs__button {
  min-width: 0;
  padding: 0 8px;
  color: var(--black-300);
  background-color: transparent;
  border-radius: 13px;
  font-size: 0.875rem;
  white-space: nowrap;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.data-tabs__button--damage {
  color: var(--black-900);
  background-color: #ffc4c6;
  box-shadow: 0 2px 8px rgb(255 80 86 / 25%);
}

.data-tabs__button--price {
  color: var(--black-900);
  background-color: #c9f5e1;
  box-shadow: 0 2px 8px rgb(47 199 139 / 25%);
}

.data_tabs__label {
  font-size: 0.75rem;
}

@media (max-width: 360px) {
  .data-tabs__button {
    gap: 4px !important;
    padding: 0 4px;
    font-size: 0.8125rem;
  }

  .data-tabs__icon {
    width: 20px;
    height: 20px;
  }
}
</style>
