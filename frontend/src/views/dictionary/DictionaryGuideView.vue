<script setup>
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";

import SecretaryGuide from "@/components/common/secretary/SecretaryGuide.vue";
import AGuideHeader from "@/components/dictionary/guides/A_GuideHeader.vue";
import BGuideTabs from "@/components/dictionary/guides/B_GuideTabs.vue";
import CComicScroller from "@/components/dictionary/guides/C_ComicScroller.vue";
import { useDictionaryCharacter } from "@/composables/useDictionaryCharacter";
import { DICTIONARY_GUIDE_CONFIGS } from "@/constants/dictionary/guides";
import BottomSheetLayout from "@/layouts/BottomSheetLayout.vue";
import { normalizeGuideConfig } from "@/utils/dictionary/guides";

const route = useRoute();
const { character, characterKey } = useDictionaryCharacter();
const activeTabId = ref("");

// 라우트 메타에 따라 등기·건축 또는 전입·확정 도감 설정을 선택합니다.
const configKey = computed(() =>
  route.meta.guideType === "register" ? "register" : "moveIn",
);
const config = computed(() =>
  normalizeGuideConfig(DICTIONARY_GUIDE_CONFIGS[configKey.value]),
);
const activeTab = computed(
  () =>
    config.value.tabs.find((tab) => tab.id === activeTabId.value) ??
    config.value.tabs[0],
);

// 도감 종류가 바뀌면 첫 탭으로 초기화합니다.
watch(
  config,
  (currentConfig) => {
    activeTabId.value = currentConfig.tabs[0]?.id ?? "";
  },
  { immediate: true },
);
</script>
<template>
  <BottomSheetLayout class="guide-page" :title-ratio="8">
    <template #header>
      <AGuideHeader
        class="w-100"
        :title="config.pageTitle"
        :current-title="config.headerTitle"
      />
    </template>

    <div class="guide-page__sheet d-flex h-100 flex-column overflow-hidden">
      <BGuideTabs v-model="activeTabId" :tabs="config.tabs" />
      <CComicScroller
        v-if="activeTab"
        :key="activeTab.id"
        class="guide-page__scroller"
        :tab="activeTab"
      />
    </div>
  </BottomSheetLayout>

  <SecretaryGuide
    :text="character.messages.comic"
    :character-type="characterKey.toUpperCase()"
    floating
  />
</template>
<style scoped>
.guide-page {
  height: 100%;
}

.guide-page__sheet {
  min-height: 0;
  border-radius: 36px 36px 0 0;
}

.guide-page__scroller {
  height: auto;
  min-height: 0;
  flex: 1;
}

@media (min-width: 768px) {
  .guide-page__sheet {
    width: min(100%, 960px);
    margin: 0 auto;
  }
}
</style>
