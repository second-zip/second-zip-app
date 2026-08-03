<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

import AFraudHeader from "@/components/dictionary/fraud/A_FraudHeader.vue";
import BFraudVideoPlayer from "@/components/dictionary/fraud/B_FraudVideoPlayer.vue";
import {
  FRAUD_DICTIONARY_COPY,
  FRAUD_TYPES,
} from "@/constants/dictionary/fraud";
import { normalizeFraudTypes } from "@/utils/dictionary/fraud";

const route = useRoute();
const fraudTypes = normalizeFraudTypes(FRAUD_TYPES);
const fraudType = computed(() =>
  fraudTypes.find((type) => type.id === route.params.typeId),
);
</script>
<template>
  <section class="video-page">
    <AFraudHeader
      :title="FRAUD_DICTIONARY_COPY.sectionTitle"
      :current-title="fraudType?.title ?? '영상'"
      back-to="/dictionary/fraud"
    />
    <nav class="video-page__toolbar" aria-label="영상 페이지 탐색">
      <RouterLink
        to="/dictionary/fraud"
        class="video-page__back"
        aria-label="전세사기 유형 목록으로 돌아가기"
        title="뒤로가기"
      >
        <span aria-hidden="true">←</span>
      </RouterLink>
    </nav>
    <BFraudVideoPlayer v-if="fraudType" :fraud-type="fraudType" />
    <p v-else class="video-page__state">해당 유형을 찾을 수 없습니다.</p>
  </section>
</template>
<style scoped>
.video-page {
  min-height: 100%;
  background: #fff;
}

.video-page__toolbar {
  display: flex;
  align-items: center;
  min-height: 52px;
  padding: 6px 20px;
}

.video-page__back {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 1px solid var(--black-100);
  border-radius: 8px;
  background: #fff;
  color: var(--black-900);
  font-size: 24px;
  line-height: 1;
  text-decoration: none;
  box-shadow: 0 2px 6px rgb(17 17 24 / 6%);
}

.video-page__back:hover {
  background: var(--blue-100);
  color: var(--blue-900);
}

.video-page__back:focus-visible {
  outline: 3px solid var(--blue-300);
  outline-offset: 2px;
}

.video-page__state {
  margin: 0;
  padding: 80px 20px;
  color: var(--black-500);
  font-size: 14px;
  text-align: center;
}
</style>
