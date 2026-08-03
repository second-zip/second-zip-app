<script setup>
import AWordHeader from "@/components/dictionary/words/A_WordHeader.vue";
import BWordList from "@/components/dictionary/words/B_WordList.vue";
import FWordGuide from "@/components/dictionary/words/F_WordGuide.vue";
import {
  WORD_DICTIONARY_COPY,
  WORD_DICTIONARY_GUIDE_IMAGE,
  WORD_DICTIONARY_ITEMS,
} from "@/constants/dictionary/words";
import { normalizeWordItems } from "@/utils/dictionary/words";

const words = normalizeWordItems(WORD_DICTIONARY_ITEMS);
</script>
<template>
  <section class="word-page">
    <AWordHeader
      :title="WORD_DICTIONARY_COPY.pageTitle"
      :section-title="WORD_DICTIONARY_COPY.sectionTitle"
    />
    <BWordList :items="words" />
    <FWordGuide
      :image="WORD_DICTIONARY_GUIDE_IMAGE"
      :message="WORD_DICTIONARY_COPY.guide"
    />
  </section>
</template>
<style scoped>
.word-page {
  min-height: 100%;
  background: #fff;
}
</style>
