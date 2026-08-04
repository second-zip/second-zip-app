<script setup>
import { nextTick, onMounted, reactive, ref } from "vue";

import { COMIC_VIEWER_OPTIONS } from "@/constants/dictionary/guides";
import { findImageContentRatio } from "@/utils/dictionary/guides";

defineProps({ tab: { type: Object, required: true } });
const emit = defineEmits(["zoom-change"]);

const scroller = ref(null);
const zoom = ref(1);
const isDragging = ref(false);
const imageMeta = reactive({});
let dragOrigin;

// 커서 위치를 기준으로 허용 범위 안에서 웹툰을 확대·축소합니다.
const setZoom = async (nextZoom, event) => {
  const previousZoom = zoom.value;
  const clampedZoom = Math.min(
    COMIC_VIEWER_OPTIONS.maxZoom,
    Math.max(COMIC_VIEWER_OPTIONS.minZoom, nextZoom),
  );
  if (clampedZoom === previousZoom) return;

  const bounds = scroller.value.getBoundingClientRect();
  const cursorX = event.clientX - bounds.left;
  const cursorY = event.clientY - bounds.top;
  const contentX = scroller.value.scrollLeft + cursorX;
  const contentY = scroller.value.scrollTop + cursorY;

  zoom.value = clampedZoom;
  emit("zoom-change", zoom.value);
  await nextTick();

  const scaleChange = clampedZoom / previousZoom;
  scroller.value.scrollLeft = contentX * scaleChange - cursorX;
  scroller.value.scrollTop = contentY * scaleChange - cursorY;
};

// Ctrl과 마우스 휠을 함께 사용한 경우에만 확대율을 변경합니다.
const handleWheel = (event) => {
  if (!event.ctrlKey) return;
  event.preventDefault();
  const direction =
    event.deltaY < 0
      ? COMIC_VIEWER_OPTIONS.zoomStep
      : -COMIC_VIEWER_OPTIONS.zoomStep;
  setZoom(Number((zoom.value + direction).toFixed(1)), event);
};

// 확대된 웹툰은 마우스 왼쪽 버튼 드래그로 이동할 수 있습니다.
const startDrag = (event) => {
  if (zoom.value <= COMIC_VIEWER_OPTIONS.minZoom || event.button !== 0) return;
  isDragging.value = true;
  dragOrigin = {
    x: event.clientX,
    y: event.clientY,
    left: scroller.value.scrollLeft,
    top: scroller.value.scrollTop,
  };
  scroller.value.setPointerCapture(event.pointerId);
};

const moveDrag = (event) => {
  if (!isDragging.value) return;
  scroller.value.scrollLeft = dragOrigin.left - (event.clientX - dragOrigin.x);
  scroller.value.scrollTop = dragOrigin.top - (event.clientY - dragOrigin.y);
};

const stopDrag = (event) => {
  if (!isDragging.value) return;
  isDragging.value = false;
  if (scroller.value.hasPointerCapture(event.pointerId)) {
    scroller.value.releasePointerCapture(event.pointerId);
  }
};

// 이미지 하단의 빈 영역을 제외한 실제 콘텐츠 비율을 적용합니다.
const trimImage = (event, imageId) => {
  const image = event.currentTarget;
  const ratio = findImageContentRatio(image);
  imageMeta[imageId] = {
    aspectRatio: `${image.naturalWidth} / ${Math.ceil(image.naturalHeight * ratio)}`,
  };
};

onMounted(() => emit("zoom-change", COMIC_VIEWER_OPTIONS.minZoom));
</script>
<template>
  <section
    :id="`${tab.id}-panel`"
    ref="scroller"
    class="comic-scroller"
    :class="{ 'is-zoomed': zoom > 1, 'is-dragging': isDragging }"
    role="tabpanel"
    :aria-labelledby="`${tab.id}-tab`"
    @wheel="handleWheel"
    @pointerdown="startDrag"
    @pointermove="moveDrag"
    @pointerup="stopDrag"
    @pointercancel="stopDrag"
  >
    <div
      v-if="tab.images.length"
      class="comic-scroller__strip"
      :style="{ width: `${zoom * 100}%` }"
    >
      <div
        v-for="image in tab.images"
        :key="image.id"
        class="comic-scroller__page"
        :style="imageMeta[image.id]"
      >
        <img
          :src="image.src"
          :alt="image.alt"
          draggable="false"
          @load="trimImage($event, image.id)"
        />
      </div>
    </div>
    <div v-else class="comic-scroller__empty" aria-label="콘텐츠 준비 중">
      <span></span><span></span><span></span>
    </div>
  </section>
</template>
<style scoped>
.comic-scroller {
  height: calc(100dvh - 68px - 53px - 64px);
  min-height: 420px;
  margin-top: 12px;
  overflow: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: var(--blue-300) transparent;
}
.comic-scroller.is-zoomed {
  cursor: grab;
  user-select: none;
}
.comic-scroller.is-dragging {
  cursor: grabbing;
}
.comic-scroller__strip {
  min-width: 100%;
  min-height: 100%;
  margin: 0 auto;
  transform-origin: top center;
}
.comic-scroller__page {
  position: relative;
  width: 100%;
  aspect-ratio: 800/10000;
  overflow: hidden;
}
.comic-scroller__page img {
  position: absolute;
  inset: 0 auto auto 0;
  display: block;
  width: 100%;
  height: auto;
  margin: 0;
  object-fit: contain;
  pointer-events: none;
}
.comic-scroller__empty {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 18px;
  padding: 34px 20px 140px;
}
.comic-scroller__empty span {
  display: block;
  height: 180px;
  border-radius: 8px;
  background: linear-gradient(100deg, #f5f6f8 20%, #eef4ff 45%, #f5f6f8 70%);
  background-size: 220% 100%;
  animation: loading 1.7s ease-in-out infinite;
}
.comic-scroller__empty span:nth-child(2) {
  height: 240px;
}
@keyframes loading {
  to {
    background-position-x: -220%;
  }
}
@media (prefers-reduced-motion: reduce) {
  .comic-scroller__empty span {
    animation: none;
  }
}
</style>
