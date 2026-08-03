<script setup>
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";

import AGuideHeader from "@/components/dictionary/guides/A_GuideHeader.vue";
import BGuideTabs from "@/components/dictionary/guides/B_GuideTabs.vue";
import CComicScroller from "@/components/dictionary/guides/C_ComicScroller.vue";
import FGuideCharacter from "@/components/dictionary/guides/F_GuideCharacter.vue";
import {
  COMIC_VIEWER_OPTIONS,
  DICTIONARY_GUIDE_CONFIGS,
} from "@/constants/dictionary/guides";
import { normalizeGuideConfig } from "@/utils/dictionary/guides";

const route = useRoute();
const activeTabId = ref("");
const zoom = ref(COMIC_VIEWER_OPTIONS.minZoom);

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

watch(
  config,
  (currentConfig) => {
    activeTabId.value = currentConfig.tabs[0]?.id ?? "";
    zoom.value = COMIC_VIEWER_OPTIONS.minZoom;
  },
  { immediate: true },
);
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
      :image="config.guideImage"
      :message="config.guideMessage"
      :compact="isGuideCompact"
      :zoom="zoom"
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
