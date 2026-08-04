<script setup>
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";

import AGuideHeader from "@/components/dictionary/guides/A_GuideHeader.vue";
import BGuideTabs from "@/components/dictionary/guides/B_GuideTabs.vue";
import CComicScroller from "@/components/dictionary/guides/C_ComicScroller.vue";
import FGuideCharacter from "@/components/dictionary/guides/F_GuideCharacter.vue";
import { useDictionaryCharacter } from "@/composables/useDictionaryCharacter";
import {
  COMIC_VIEWER_OPTIONS,
  DICTIONARY_GUIDE_CONFIGS,
} from "@/constants/dictionary/guides";
import { normalizeGuideConfig } from "@/utils/dictionary/guides";

const route = useRoute();
const { character } = useDictionaryCharacter();
const activeTabId = ref("");
const zoom = ref(COMIC_VIEWER_OPTIONS.minZoom);
const isGuideDismissed = ref(false);

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
const isGuideCompact = computed(
  () => zoom.value > COMIC_VIEWER_OPTIONS.compactGuideThreshold,
);

// 도감 종류가 바뀌면 첫 탭과 뷰어 상태를 초기화합니다.
watch(
  config,
  (currentConfig) => {
    activeTabId.value = currentConfig.tabs[0]?.id ?? "";
    zoom.value = COMIC_VIEWER_OPTIONS.minZoom;
    isGuideDismissed.value = false;
  },
  { immediate: true },
);

// 다른 웹툰 탭으로 이동하면 숨긴 안내를 다시 표시합니다.
watch(activeTabId, () => {
  isGuideDismissed.value = false;
});
</script>
<template>
  <section class="guide-page">
    <AGuideHeader
      :title="config.pageTitle"
      :current-title="config.headerTitle"
    />
    <BGuideTabs v-model="activeTabId" :tabs="config.tabs" />
    <CComicScroller
      v-if="activeTab"
      :key="activeTab.id"
      :tab="activeTab"
      @zoom-change="zoom = $event"
    />
    <FGuideCharacter
      :image="character.guideImage"
      :message="character.messages.comic"
      :compact="isGuideCompact"
      :dismissed="isGuideDismissed"
      :zoom="zoom"
      @toggle="isGuideDismissed = !isGuideDismissed"
    />
  </section>
</template>
<style scoped>
.guide-page {
  height: 100%;
  overflow: hidden;
  background: #fff;
}
</style>
