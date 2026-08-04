<script setup>
import AWordHeader from "@/components/dictionary/words/A_WordHeader.vue";
import BWordList from "@/components/dictionary/words/B_WordList.vue";
import FWordGuide from "@/components/dictionary/words/F_WordGuide.vue";
import { useDictionaryCharacter } from "@/composables/useDictionaryCharacter";
import { WORD_DICTIONARY_COPY, WORD_DICTIONARY_ITEMS } from "@/constants/dictionary/words";
import { normalizeWordItems } from "@/utils/dictionary/words";

// 고정 용어 데이터를 화면 출력 형식으로 정규화합니다.
const words = normalizeWordItems(WORD_DICTIONARY_ITEMS);
const { character } = useDictionaryCharacter();
</script>
<template>
  <section class="word-page">
    <AWordHeader
      :title="WORD_DICTIONARY_COPY.pageTitle"
      :section-title="WORD_DICTIONARY_COPY.sectionTitle"
    />
    <BWordList :items="words" />
    <FWordGuide
      :image="character.guideImage"
      :message="character.messages.words"
    />
  </section>
</template>
<style scoped>
.word-page {
  min-height: 100%;
  background: #fff;
}
</style>
