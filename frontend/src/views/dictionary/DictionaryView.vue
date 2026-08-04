<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";

import ADictionaryHeader from "@/components/dictionary/main/A_DictionaryHeader.vue";
import BWordCard from "@/components/dictionary/main/B_WordCard.vue";
import CFraudCard from "@/components/dictionary/main/C_FraudCard.vue";
import DRegisterCard from "@/components/dictionary/main/D_RegisterCard.vue";
import EMoveInCard from "@/components/dictionary/main/E_MoveInCard.vue";
import FDictionaryGuide from "@/components/dictionary/main/F_DictionaryGuide.vue";
import { useDictionaryCharacter } from "@/composables/useDictionaryCharacter";
import {
  DICTIONARY_MAIN_CARDS,
  DICTIONARY_MAIN_COPY,
} from "@/constants/dictionary/main";
import { normalizeDictionaryCards } from "@/utils/dictionary/main";

const router = useRouter();
const { character } = useDictionaryCharacter();

// 메뉴 데이터에 현재 비서 캐릭터의 구역별 이미지를 결합합니다.
const cards = computed(() =>
  normalizeDictionaryCards(DICTIONARY_MAIN_CARDS).map((card) => ({
    ...card,
    image: character.value.cardImages[card.imageKey],
  })),
);

// 선택한 메뉴 카드에 설정된 이름으로 하위 도감 화면을 엽니다.
const selectSection = (sectionId) => {
  const selectedCard = cards.value.find((card) => card.id === sectionId);
  if (!selectedCard?.routeName) return;

  router.push({ name: selectedCard.routeName });
};
</script>
<template>
  <section class="dictionary-page">
    <ADictionaryHeader
      :title="DICTIONARY_MAIN_COPY.title"
      :description="DICTIONARY_MAIN_COPY.description"
    />
    <div class="dictionary-page__grid">
      <BWordCard :card="cards[0]" @select="selectSection" />
      <CFraudCard :card="cards[1]" @select="selectSection" />
      <DRegisterCard :card="cards[2]" @select="selectSection" />
      <EMoveInCard :card="cards[3]" @select="selectSection" />
    </div>
    <FDictionaryGuide
      :image="character.guideImage"
      :message="character.messages.main"
    />
  </section>
</template>
<style scoped>
.dictionary-page {
  min-height: 100%;
  background: #fff;
}
.dictionary-page__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 42px 18px 16px;
}

@media (max-width: 359px) {
  .dictionary-page__grid {
    gap: 10px;
    padding-right: 12px;
    padding-left: 12px;
  }
}
</style>
