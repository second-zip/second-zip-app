<script setup>
// 캐릭터나 말풍선을 누르면 안내를 축소하거나 다시 표시합니다.
defineProps({
  image: { type: String, required: true },
  message: { type: String, required: true },
  compact: { type: Boolean, default: false },
  dismissed: { type: Boolean, default: false },
  zoom: { type: Number, default: 1 },
});
defineEmits(["toggle"]);
</script>
<template>
  <aside
    class="guide-character"
    :class="{ 'guide-character--compact': compact || dismissed }"
  >
    <button
      type="button"
      class="guide-character__avatar"
      :aria-label="dismissed ? '확대 안내 다시 보기' : '확대 안내 숨기기'"
      @click="$emit('toggle')"
    >
      <img :src="image" alt="" />
    </button>
    <button
      v-if="!dismissed"
      type="button"
      class="guide-character__message"
      aria-label="확대 안내 숨기기"
      @click="$emit('toggle')"
    >
      {{ compact ? `${Math.round(zoom * 100)}%` : message }}
    </button>
  </aside>
</template>
<style scoped>
.guide-character {
  position: fixed;
  bottom: 64px;
  left: 50%;
  z-index: 1020;
  display: flex;
  width: min(100%, 402px);
  align-items: center;
  gap: 8px;
  padding: 8px 12px 10px;
  transform: translateX(-50%);
  pointer-events: none;
}
.guide-character__avatar {
  width: 94px;
  height: 85px;
  flex: 0 0 94px;
  overflow: hidden;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: #78d8d5;
  filter: drop-shadow(0 4px 4px rgb(17 17 24 / 10%));
  pointer-events: auto;
}
.guide-character img {
  width: 100%;
  height: 112%;
  object-fit: contain;
  object-position: center top;
}
.guide-character__message {
  flex: 1;
  margin: 18px 0 0;
  padding: 14px 12px;
  text-align: left;
  white-space: pre-line;
  border: 1px solid #67d1d0;
  border-radius: 14px;
  background: #e9f8f7;
  color: var(--black-900);
  font-family: "BM Kkubulim", sans-serif;
  font-size: 11px;
  line-height: 1.45;
  pointer-events: auto;
  cursor: pointer;
}
.guide-character--compact {
  right: max(12px, calc((100% - 402px) / 2 + 12px));
  left: auto;
  width: auto;
  gap: 5px;
  padding: 6px;
  transform: none;
}
.guide-character--compact .guide-character__avatar {
  width: 48px;
  height: 44px;
  flex-basis: 48px;
}
.guide-character--compact .guide-character__message {
  flex: none;
  margin: 0;
  padding: 7px 9px;
  border-radius: 10px;
  font-family: inherit;
  font-size: 11px;
  font-weight: 700;
}
</style>
