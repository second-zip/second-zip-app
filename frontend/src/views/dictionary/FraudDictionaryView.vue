<script setup>
import { useRouter } from "vue-router";

import AFraudHeader from "@/components/dictionary/fraud/A_FraudHeader.vue";
import BFraudTypeList from "@/components/dictionary/fraud/B_FraudTypeList.vue";
import FFraudGuide from "@/components/dictionary/fraud/F_FraudGuide.vue";
import {
  FRAUD_DICTIONARY_COPY,
  FRAUD_DICTIONARY_GUIDE_IMAGE,
  FRAUD_TYPES,
} from "@/constants/dictionary/fraud";
import { normalizeFraudTypes } from "@/utils/dictionary/fraud";

const router = useRouter();
const fraudTypes = normalizeFraudTypes(FRAUD_TYPES);

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
      :image="FRAUD_DICTIONARY_GUIDE_IMAGE"
      :message="FRAUD_DICTIONARY_COPY.guide"
    />
  </section>
</template>
<style scoped>
.fraud-page {
  min-height: 100%;
  background: #fff;
}
</style>
