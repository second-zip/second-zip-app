<script setup>
import { computed, ref, watch } from 'vue';

import PlusIcon from '@/assets/icons/report/plus-white-14.svg';
import PlusDisableIcon from '@/assets/icons/report/plus-gray-14.svg';

import { searchAddresses } from '@/api/address';
import ReportCreateBox from '@/components/report/ReportCreateBox.vue';
import ReportAddressSearch from './ReportAddressSearch.vue';
import ReportDetailAddressInput from './ReportDetailAddressInput.vue';
import ReportDepositInput from './ReportDepositInput.vue';

const emit = defineEmits(['submit']);

const addressKeyword = ref('');
const addressResults = ref([]);
const selectedAddress = ref(null);
const isAddressLoading = ref(false);
const addressErrorMessage = ref('');
const dong = ref('');
const ho = ref('');
const deposit = ref('');

let addressSearchRequestId = 0;

const getAddressLabel = (address) =>
  address?.roadAddress || address?.jibunAddress || '';

const handleSearchAddress = async (query) => {
  const keyword = query.trim();
  const requestId = ++addressSearchRequestId;

  if (!keyword) {
    addressResults.value = [];
    addressErrorMessage.value = '';
    return;
  }

  isAddressLoading.value = true;
  addressErrorMessage.value = '';

  try {
    const results = await searchAddresses(keyword);

    if (requestId === addressSearchRequestId) {
      addressResults.value = results;
    }
  } catch (error) {
    if (requestId !== addressSearchRequestId) return;

    addressResults.value = [];
    addressErrorMessage.value = '주소를 검색하지 못했습니다.';
  } finally {
    if (requestId === addressSearchRequestId) {
      isAddressLoading.value = false;
    }
  }
};

const handleSelectAddress = (address) => {
  selectedAddress.value = address;
};

const handleClearAddress = () => {
  addressSearchRequestId += 1;
  addressKeyword.value = '';
  addressResults.value = [];
  selectedAddress.value = null;
  isAddressLoading.value = false;
  addressErrorMessage.value = '';
};

const validateDeposit = (value) =>
  Number.isFinite(Number(value)) && Number(value) > 0;

const validateReport = computed(() => {
  return (
    Boolean(selectedAddress.value?.addressId) && validateDeposit(deposit.value)
  );
});

watch(addressKeyword, (value) => {
  if (
    selectedAddress.value &&
    value !== getAddressLabel(selectedAddress.value)
  ) {
    selectedAddress.value = null;
  }
});

const handleSubmit = () => {
  emit('submit', {
    address: selectedAddress.value,
    addressKeyword: addressKeyword.value,
    dong: dong.value.trim(),
    ho: ho.value.trim(),
    deposit: Number(deposit.value),
    validateReport: validateReport.value,
  });
};
</script>

<template>
  <form
    class="report-create-form d-flex flex-column"
    @submit.prevent="handleSubmit"
  >
    <ReportCreateBox :show-button="false" embedded />

    <ReportAddressSearch
      v-model="addressKeyword"
      :results="addressResults"
      :is-loading="isAddressLoading"
      :error-message="addressErrorMessage"
      @search="handleSearchAddress"
      @select="handleSelectAddress"
      @clear="handleClearAddress"
    />

    <ReportDetailAddressInput v-model:dong="dong" v-model:ho="ho" />

    <ReportDepositInput v-model="deposit" />

    <div class="report-create-form__button-wrap d-flex justify-content-center">
      <button
        type="submit"
        :disabled="!validateReport"
        class="report-create-form__submit d-inline-flex align-items-center justify-content-center border-0 fw-semibold"
      >
        <img :src="validateReport ? PlusIcon : PlusDisableIcon" alt="" />
        <span>리포트 생성하기</span>
      </button>
    </div>
  </form>
</template>

<style scoped>
.report-create-form {
  min-height: 100%;
  padding: 0 24px 20px;
  background-color: var(--blue-100);
  border-radius: 24px;
  box-sizing: border-box;
}

.report-create-form__button-wrap {
  margin-top: auto;
  padding-top: 32px;
}

.report-create-form__submit {
  height: 40px;
  padding: 0 20px;
  gap: 7px;
  color: #fff;
  font-size: 0.8125rem;
  background-color: var(--blue-900);
  border-radius: 14px;
}

.report-create-form__submit:disabled {
  background-color: var(--black-100);
  color: var(--black-300);
  cursor: not-allowed;
}

.report-create-form__submit img {
  width: 14px;
  height: 14px;
}

.report-create-form__submit:focus-visible {
  outline: 2px solid var(--blue-700);
  outline-offset: 2px;
}
</style>
