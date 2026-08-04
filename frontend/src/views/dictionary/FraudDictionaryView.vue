<script setup>
import { useRouter } from "vue-router";

import AFraudHeader from "@/components/dictionary/fraud/A_FraudHeader.vue";
import BFraudTypeList from "@/components/dictionary/fraud/B_FraudTypeList.vue";
import FFraudGuide from "@/components/dictionary/fraud/F_FraudGuide.vue";
import { useDictionaryCharacter } from "@/composables/useDictionaryCharacter";
import { FRAUD_DICTIONARY_COPY, FRAUD_TYPES } from "@/constants/dictionary/fraud";
import { normalizeFraudTypes } from "@/utils/dictionary/fraud";

const router = useRouter();
const fraudTypes = normalizeFraudTypes(FRAUD_TYPES);
const { character } = useDictionaryCharacter();

// 선택한 사기 유형의 숏폼 상세 화면으로 이동합니다.
const openVideo = (typeId) => {
  router.push({
    name: "dictionary-fraud-video",
    params: { typeId },
  });
};
</script>
<template>
  <section class="fraud-page">
    <AFraudHeader
      :title="FRAUD_DICTIONARY_COPY.pageTitle"
      :current-title="FRAUD_DICTIONARY_COPY.sectionTitle"
    />
    <BFraudTypeList :types="fraudTypes" @play="openVideo" />
    <FFraudGuide
      :image="character.guideImage"
      :message="character.messages.fraud"
    />
  </section>
</template>
<style scoped>
.fraud-page {
  min-height: 100%;
  background: #fff;
}
</style>
