<script setup>
import { computed } from 'vue';

import ClearIcon from '@/assets/icons/report/cancel-gray-18.svg';

const props = defineProps({
  dong: {
    type: String,
    default: '',
  },
  ho: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['update:dong', 'update:ho']);

const hasDong = computed(() => props.dong.length > 0);
const hasHo = computed(() => props.ho.length > 0);

const updateDong = (event) => {
  emit('update:dong', event.target.value);
};

const updateHo = (event) => {
  emit('update:ho', event.target.value);
};

const clearDong = () => {
  emit('update:dong', '');
};

const clearHo = () => {
  emit('update:ho', '');
};
</script>

<template>
  <section class="detail-address-form-box">
    <h3 class="form-section-title fw-bold mb-3">
      <span>상세 주소</span>를 입력해주세요
      <small class="form-section-title__optional fw-medium">선택</small>
    </h3>

    <div class="detail-address-fields d-grid">
      <div class="detail-address-field d-flex align-items-center">
        <div
          class="detail-address-input-box d-flex align-items-center flex-grow-1"
        >
          <input
            :value="dong"
            type="text"
            inputmode="numeric"
            class="detail-address-input flex-grow-1 border-0 fw-semibold"
            placeholder="예) 101"
            aria-label="동 입력"
            @input="updateDong"
          />

          <button
            v-if="hasDong"
            type="button"
            class="detail-address-clear-button d-flex align-items-center justify-content-center border-0 p-0"
            aria-label="동 지우기"
            @click="clearDong"
          >
            <img :src="ClearIcon" alt="" />
          </button>
        </div>

        <span class="detail-address-unit fw-bold">동</span>
      </div>

      <div class="detail-address-field d-flex align-items-center">
        <div
          class="detail-address-input-box d-flex align-items-center flex-grow-1"
        >
          <input
            :value="ho"
            type="text"
            inputmode="numeric"
            class="detail-address-input flex-grow-1 border-0 fw-semibold"
            placeholder="예) 1203"
            aria-label="호수 입력"
            @input="updateHo"
          />

          <button
            v-if="hasHo"
            type="button"
            class="detail-address-clear-button d-flex align-items-center justify-content-center border-0 p-0"
            aria-label="호수 지우기"
            @click="clearHo"
          >
            <img :src="ClearIcon" alt="" />
          </button>
        </div>

        <span class="detail-address-unit fw-bold">호</span>
      </div>
    </div>
  </section>
</template>

<style scoped src="./ReportDetailAddressInput.css" />
