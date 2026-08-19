<script setup>
import { useRouter } from "vue-router";

import SecretaryGuide from "@/components/common/secretary/SecretaryGuide.vue";
import AFraudHeader from "@/components/dictionary/fraud/A_FraudHeader.vue";
import BFraudTypeList from "@/components/dictionary/fraud/B_FraudTypeList.vue";
import { useDictionaryCharacter } from "@/composables/useDictionaryCharacter";
import { FRAUD_DICTIONARY_COPY, FRAUD_TYPES } from "@/constants/dictionary/fraud";
import BottomSheetLayout from "@/layouts/BottomSheetLayout.vue";
import { normalizeFraudTypes } from "@/utils/dictionary/fraud";

const router = useRouter();
const fraudTypes = normalizeFraudTypes(FRAUD_TYPES);
const { character, characterKey } = useDictionaryCharacter();

// 선택한 사기 유형의 숏폼 상세 화면으로 이동합니다.
const openVideo = (typeId) => {
  router.push({
    name: "dictionary-fraud-video",
    params: { typeId },
  });
};
</script>
<template>
  <BottomSheetLayout class="fraud-page" :title-ratio="8">
    <template #header>
      <AFraudHeader
        class="w-100"
        :title="FRAUD_DICTIONARY_COPY.pageTitle"
        :current-title="FRAUD_DICTIONARY_COPY.sectionTitle"
      />
    </template>

    <div class="fraud-page__scroll w-100 h-100 overflow-y-auto">
      <BFraudTypeList :types="fraudTypes" @play="openVideo" />
    </div>
  </BottomSheetLayout>

  <SecretaryGuide
    :text="character.messages.fraud"
    :character-type="characterKey.toUpperCase()"
    floating
  />
</template>
<style scoped>
.fraud-page {
  height: 100%;
}

.fraud-page__scroll {
  border-radius: 36px 36px 0 0;
  overscroll-behavior: contain;
}

@media (min-width: 768px) {
  .fraud-page__scroll {
    width: min(100%, 1040px) !important;
    margin: 0 auto;
  }
}

@media (min-width: 1024px) {
  .fraud-page__scroll :deep(.fraud-list) {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-items: start;
    gap: 20px;
    padding-inline: 32px;
  }
}
</style>
