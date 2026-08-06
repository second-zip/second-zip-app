<script setup>
import { ref } from 'vue';

import PlusIcon from '@/assets/icons/report/plus-white-14.svg';

import ReportCreateBox from '@/components/report/ReportCreateBox.vue';
import ReportAddressSearch from './ReportAddressSearch.vue';
import ReportDepositInput from './ReportDepositInput.vue';

const emit = defineEmits(['submit']);

const addressKeyword = ref('');
const selectedAddress = ref(null);
const deposit = ref('');

const addressResults = [
  {
    id: 1,
    roadAddress: '오금로 34길 4',
    jibunAddress: '서울특별시 송파구 가락동 3-15',
  },
  {
    id: 2,
    roadAddress: '오금로 34길 4-3(가락동, 목화5차빌라)',
    jibunAddress: '서울특별시 송파구 가락동 4-1 목화5차빌라',
  },
  {
    id: 3,
    roadAddress: '오금로 34길 4-4(가락동)',
    jibunAddress: '서울특별시 송파구 가락동 3-14',
  },
  {
    id: 4,
    roadAddress: '오금로 34길 6',
    jibunAddress: '서울특별시 송파구 가락동 3-18',
  },
  {
    id: 5,
    roadAddress: '오금로 34길 8',
    jibunAddress: '서울특별시 송파구 가락동 3-20',
  },
];

const handleSelectAddress = (address) => {
  selectedAddress.value = address;
};

const handleSubmit = () => {
  emit('submit', {
    address: selectedAddress.value,
    addressKeyword: addressKeyword.value,
    deposit: Number(deposit.value),
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
      @select="handleSelectAddress"
    />

    <ReportDepositInput v-model="deposit" />

    <div class="report-create-form__button-wrap d-flex justify-content-center">
      <button
        type="submit"
        class="report-create-form__submit d-inline-flex align-items-center justify-content-center border-0 fw-semibold"
      >
        <img :src="PlusIcon" alt="" />
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

.report-create-form__submit img {
  width: 14px;
  height: 14px;
}

.report-create-form__submit:focus-visible {
  outline: 2px solid var(--blue-700);
  outline-offset: 2px;
}
</style>
