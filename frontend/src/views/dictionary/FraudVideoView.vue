<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

import SecretaryGuide from "@/components/common/secretary/SecretaryGuide.vue";
import AFraudHeader from "@/components/dictionary/fraud/A_FraudHeader.vue";
import BFraudVideoPlayer from "@/components/dictionary/fraud/B_FraudVideoPlayer.vue";
import { useDictionaryCharacter } from "@/composables/useDictionaryCharacter";
import {
  FRAUD_DICTIONARY_COPY,
  FRAUD_TYPES,
} from "@/constants/dictionary/fraud";
import BottomSheetLayout from "@/layouts/BottomSheetLayout.vue";
import { normalizeFraudTypes } from "@/utils/dictionary/fraud";

const route = useRoute();
const { character, characterKey } = useDictionaryCharacter();
const fraudTypes = normalizeFraudTypes(FRAUD_TYPES);
const fraudType = computed(() =>
  fraudTypes.find((type) => type.id === route.params.typeId),
);
</script>
<template>
  <BottomSheetLayout class="video-page" :title-ratio="8">
    <template #header>
      <AFraudHeader
        class="w-100"
        :title="FRAUD_DICTIONARY_COPY.sectionTitle"
        :current-title="fraudType?.title ?? '영상'"
        back-to="/dictionary/fraud"
      />
    </template>

    <div class="video-page__scroll w-100 h-100 overflow-y-auto">
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
      <p v-else class="video-page__state">
        해당 유형을 찾을 수 없습니다.
      </p>
    </div>
  </BottomSheetLayout>

  <SecretaryGuide
    :text="character.messages.fraud"
    :character-type="characterKey.toUpperCase()"
    floating
  />
</template>
<style scoped>
.video-page {
  height: 100%;
}

.video-page__scroll {
  border-radius: 36px 36px 0 0;
  overscroll-behavior: contain;
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

@media (min-width: 768px) {
  .video-page__scroll {
    width: min(100%, 900px) !important;
    margin: 0 auto;
  }
}
</style>
