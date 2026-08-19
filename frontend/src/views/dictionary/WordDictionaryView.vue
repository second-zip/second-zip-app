<script setup>
import AWordHeader from "@/components/dictionary/words/A_WordHeader.vue";
import BWordList from "@/components/dictionary/words/B_WordList.vue";
import SecretaryGuide from "@/components/common/secretary/SecretaryGuide.vue";
import { useDictionaryCharacter } from "@/composables/useDictionaryCharacter";
import { WORD_DICTIONARY_COPY, WORD_DICTIONARY_ITEMS } from "@/constants/dictionary/words";
import BottomSheetLayout from "@/layouts/BottomSheetLayout.vue";
import { normalizeWordItems } from "@/utils/dictionary/words";

// 고정 용어 데이터를 화면 출력 형식으로 정규화합니다.
const words = normalizeWordItems(WORD_DICTIONARY_ITEMS);
const { character, characterKey } = useDictionaryCharacter();
</script>
<template>
  <BottomSheetLayout class="word-page" :title-ratio="8">
    <template #header>
      <AWordHeader
        class="w-100"
        :title="WORD_DICTIONARY_COPY.pageTitle"
        :section-title="WORD_DICTIONARY_COPY.sectionTitle"
      />
    </template>

    <div class="word-page__scroll w-100 h-100 overflow-y-auto">
      <BWordList :items="words" />
    </div>
  </BottomSheetLayout>

  <SecretaryGuide
    :text="character.messages.words"
    :character-type="characterKey.toUpperCase()"
    floating
  />
</template>
<style scoped>
.word-page {
  height: 100%;
}

.word-page__scroll {
  border-radius: 36px 36px 0 0;
  overscroll-behavior: contain;
}

@media (min-width: 768px) {
  .word-page__scroll {
    width: min(100%, 1000px) !important;
    margin: 0 auto;
  }
}

@media (min-width: 1024px) {
  .word-page__scroll :deep(.word-list) {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-items: start;
    gap: 16px 24px;
    padding-inline: 32px;
  }
}
</style>
