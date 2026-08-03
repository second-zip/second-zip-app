<script setup>
import { useRouter } from "vue-router";

import ADictionaryHeader from "@/components/dictionary/main/A_DictionaryHeader.vue";
import BWordCard from "@/components/dictionary/main/B_WordCard.vue";
import CFraudCard from "@/components/dictionary/main/C_FraudCard.vue";
import DRegisterCard from "@/components/dictionary/main/D_RegisterCard.vue";
import EMoveInCard from "@/components/dictionary/main/E_MoveInCard.vue";
import FDictionaryGuide from "@/components/dictionary/main/F_DictionaryGuide.vue";
import {
  DICTIONARY_GUIDE_IMAGE,
  DICTIONARY_MAIN_CARDS,
  DICTIONARY_MAIN_COPY,
} from "@/constants/dictionary/main";
import { normalizeDictionaryCards } from "@/utils/dictionary/main";

const router = useRouter();
const cards = normalizeDictionaryCards(DICTIONARY_MAIN_CARDS);

const selectSection = (sectionId) => {
  const selectedCard = cards.find((card) => card.id === sectionId);
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
      :image="DICTIONARY_GUIDE_IMAGE"
      :message="DICTIONARY_MAIN_COPY.guide"
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
