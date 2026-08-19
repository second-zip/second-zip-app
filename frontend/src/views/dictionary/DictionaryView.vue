<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";

import ADictionaryHeader from "@/components/dictionary/main/A_DictionaryHeader.vue";
import BWordCard from "@/components/dictionary/main/B_WordCard.vue";
import CFraudCard from "@/components/dictionary/main/C_FraudCard.vue";
import DRegisterCard from "@/components/dictionary/main/D_RegisterCard.vue";
import EMoveInCard from "@/components/dictionary/main/E_MoveInCard.vue";
import SecretaryGuide from "@/components/common/secretary/SecretaryGuide.vue";
import { useDictionaryCharacter } from "@/composables/useDictionaryCharacter";
import {
  DICTIONARY_MAIN_CARDS,
  DICTIONARY_MAIN_COPY,
} from "@/constants/dictionary/main";
import BottomSheetLayout from "@/layouts/BottomSheetLayout.vue";
import { normalizeDictionaryCards } from "@/utils/dictionary/main";

const router = useRouter();
const { character, characterKey } = useDictionaryCharacter();

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
  <BottomSheetLayout class="dictionary-page" :title-ratio="20">
    <template #header>
      <ADictionaryHeader
        class="w-100"
        :title="DICTIONARY_MAIN_COPY.title"
        :description="DICTIONARY_MAIN_COPY.description"
      />
    </template>

    <div class="dictionary-page__scroll w-100 h-100 overflow-y-auto">
      <div class="dictionary-page__grid">
        <BWordCard :card="cards[0]" @select="selectSection" />
        <CFraudCard :card="cards[1]" @select="selectSection" />
        <DRegisterCard :card="cards[2]" @select="selectSection" />
        <EMoveInCard :card="cards[3]" @select="selectSection" />
      </div>
      <SecretaryGuide
        class="dictionary-guide"
        :text="character.messages.main"
        :character-type="characterKey.toUpperCase()"
      />
    </div>
  </BottomSheetLayout>
</template>
<style scoped>
.dictionary-page {
  height: 100%;
}

.dictionary-page__scroll {
  border-radius: 36px 36px 0 0;
  overscroll-behavior: contain;
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

@media (min-width: 768px) {
  .dictionary-page__scroll {
    width: min(100%, 1100px) !important;
    margin: 0 auto;
  }

  .dictionary-page__grid {
    width: min(100%, 402px);
    margin: 0 auto;
  }

  .dictionary-guide {
    max-width: 402px;
    margin: 8px auto 24px;
  }
}
</style>
