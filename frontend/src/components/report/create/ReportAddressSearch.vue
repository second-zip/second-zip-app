<script setup>
import { computed, ref, watch } from 'vue';

import ClearIcon from '@/assets/icons/report/cancel-gray-18.svg';
import SearchIcon from '@/assets/icons/report/search-gray-18.svg';

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  results: {
    type: Array,
    default: () => [],
  },
  isLoading: {
    type: Boolean,
    default: false,
  },
  errorMessage: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['update:modelValue', 'search', 'select', 'clear']);

const isResultsOpen = ref(false);
const hasSearched = ref(false);

const hasKeyword = computed(() => props.modelValue.trim().length > 0);

const showResults = computed(
  () =>
    isResultsOpen.value &&
    (hasSearched.value || props.isLoading || Boolean(props.errorMessage)),
);

const updateKeyword = (event) => {
  emit('update:modelValue', event.target.value);
  hasSearched.value = false;
  isResultsOpen.value = false;
};

const clearKeyword = () => {
  emit('clear');
  hasSearched.value = false;
  isResultsOpen.value = false;
};

const searchAddress = () => {
  if (!hasKeyword.value) return;

  hasSearched.value = true;
  isResultsOpen.value = true;
  emit('search', props.modelValue.trim());
};

const selectAddress = (address) => {
  emit('update:modelValue', address.roadAddress || address.jibunAddress);
  emit('select', address);
  hasSearched.value = false;
  isResultsOpen.value = false;
};

watch(
  () => props.modelValue,
  (value) => {
    if (!value.trim()) {
      hasSearched.value = false;
      isResultsOpen.value = false;
    }
  },
);
</script>

<template>
  <section>
    <h3 class="form-section-title fw-bold mb-3">
      <span>주소</span>를 입력해 주세요
    </h3>

    <div class="address-search overflow-hidden">
      <div class="address-search__input-row d-flex align-items-center">
        <input
          :value="modelValue"
          type="text"
          class="address-search__input flex-grow-1 border-0"
          placeholder="도로명 또는 지번 주소"
          aria-label="주소 검색어"
          @input="updateKeyword"
          @keydown.enter.prevent="searchAddress"
        />

        <button
          v-if="hasKeyword"
          type="button"
          class="address-search__icon-button d-flex align-items-center justify-content-center border-0 p-0"
          aria-label="주소 검색어 지우기"
          @click="clearKeyword"
        >
          <img :src="ClearIcon" alt="" />
        </button>

        <button
          type="button"
          class="address-search__icon-button d-flex align-items-center justify-content-center border-0 p-0"
          aria-label="주소 검색"
          @click="searchAddress"
        >
          <img :src="SearchIcon" alt="" />
        </button>
      </div>

      <div
        v-if="showResults"
        class="address-search__results"
        role="listbox"
        aria-label="주소 검색 결과"
      >
        <p
          v-if="isLoading"
          class="address-search__status mb-0"
          role="status"
        >
          주소를 검색하고 있어요.
        </p>

        <p
          v-else-if="errorMessage"
          class="address-search__status address-search__status--error mb-0"
          role="alert"
        >
          {{ errorMessage }}
        </p>

        <template v-else-if="results.length > 0">
          <button
            v-for="(address, index) in results"
            :key="address.addressId ?? index"
            type="button"
            class="address-search__result w-100 border-0 text-start"
            @click="selectAddress(address)"
          >
            <strong
              class="address-search__road d-block fw-medium text-truncate"
            >
              {{ address.roadAddress || address.jibunAddress }}
            </strong>

            <span
              v-if="address.roadAddress && address.jibunAddress"
              class="address-search__jibun d-block fw-normal text-truncate"
            >
              {{ address.jibunAddress }}
            </span>
          </button>
        </template>

        <p v-else class="address-search__status mb-0" role="status">
          검색 결과가 없습니다.
        </p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.form-section-title {
  color: var(--black-900);
  font-size: 1.25rem;
  line-height: 1.4;
  letter-spacing: -0.04em;
}

.form-section-title span {
  color: var(--blue-900);
}

.address-search {
  background-color: #fff;
  border: 1px solid var(--black-100);
  border-radius: 16px;
}

.address-search__input-row {
  height: 54px;
  padding: 0 14px 0 16px;
  gap: 8px;
}

.address-search__input {
  min-width: 0;
  color: var(--black-900);
  font-size: 1rem;
  background-color: transparent;
  outline: none;
}

.address-search__input::placeholder {
  color: var(--black-300);
}

.address-search__icon-button {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  background-color: transparent;
  border-radius: 8px;
}

.address-search__icon-button img {
  width: 20px;
  height: 20px;
}

.address-search__icon-button:focus-visible {
  outline: 2px solid var(--blue-700);
  outline-offset: 1px;
}

.address-search__results {
  max-height: 220px;
  overflow-y: auto;
  overscroll-behavior: contain;
  border-top: 1px solid var(--black-100);
}

.address-search__status {
  padding: 18px 16px;
  color: var(--black-500);
  font-size: 0.875rem;
  line-height: 1.4;
}

.address-search__status--error {
  color: var(--black-700);
}

.address-search__result {
  min-height: 72px;
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid var(--black-100) !important;
}

.address-search__result:last-child {
  border-bottom: 0 !important;
}

.address-search__result:hover,
.address-search__result:focus-visible {
  background-color: var(--blue-100);
  outline: none;
}

.address-search__road {
  color: var(--black-900);
  font-size: 0.9375rem;
  line-height: 1.4;
}

.address-search__jibun {
  margin-top: 2px;
  color: var(--black-500);
  font-size: 0.8125rem;
  line-height: 1.4;
}
</style>
