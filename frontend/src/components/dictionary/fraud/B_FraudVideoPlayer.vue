<script setup>
import { computed } from "vue";

import { isYouTubeEmbedUrl } from "@/utils/dictionary/fraud";

const props = defineProps({ fraudType: { type: Object, required: true } });

// 유튜브 링크는 iframe으로, 그 외 영상 파일은 video 요소로 재생합니다.
const isYouTubeVideo = computed(() =>
  isYouTubeEmbedUrl(props.fraudType.videoSrc),
);
</script>
<template>
  <section class="video-section">
    <div class="video-frame">
      <iframe
        v-if="isYouTubeVideo"
        :src="fraudType.videoSrc"
        :title="`${fraudType.title} 영상`"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
        allowfullscreen
      ></iframe>
      <video
        v-else-if="fraudType.videoSrc"
        :src="fraudType.videoSrc"
        controls
        playsinline
        preload="metadata"
      ></video>
      <div v-else class="video-frame__empty">
        <i aria-hidden="true"></i><span>영상 준비 중</span>
      </div>
    </div>
    <h2>{{ fraudType.title }}</h2>
    <p>{{ fraudType.description }}</p>
  </section>
</template>
<style scoped>
.video-section {
  padding: 0 24px 100px;
}
.video-frame {
  width: min(100%, 300px);
  aspect-ratio: 9/16;
  margin: 0 auto 20px;
  overflow: hidden;
  background: #111118;
  box-shadow: 0 8px 24px rgb(17 17 24 / 14%);
}
video,
iframe {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
}
video {
  object-fit: contain;
}
.video-frame__empty {
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: var(--black-300);
  font-size: 13px;
}
.video-frame__empty i {
  width: 42px;
  height: 42px;
  border: 3px solid #f2a6ab;
  border-radius: 50%;
}
h2 {
  margin: 0 0 10px;
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0;
}
p {
  margin: 0;
  color: var(--black-500);
  font-size: 13px;
  line-height: 1.7;
  word-break: keep-all;
}
</style>
