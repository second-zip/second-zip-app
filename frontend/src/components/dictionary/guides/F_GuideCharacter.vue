<script setup>
defineProps({
  image: { type: String, required: true },
  message: { type: String, required: true },
  compact: { type: Boolean, default: false },
  zoom: { type: Number, default: 1 },
});
</script>
<template>
  <aside
    class="guide-character"
    :class="{ 'guide-character--compact': compact }"
  >
    <div><img :src="image" alt="" /></div>
    <p>{{ compact ? `${Math.round(zoom * 100)}%` : message }}</p>
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
.guide-character div {
  width: 94px;
  height: 85px;
  flex: 0 0 94px;
  overflow: hidden;
  border-radius: 50%;
  background: #78d8d5;
  filter: drop-shadow(0 4px 4px rgb(17 17 24 / 10%));
}
.guide-character img {
  width: 100%;
  height: 112%;
  object-fit: contain;
  object-position: center top;
}
p {
  flex: 1;
  margin: 18px 0 0;
  padding: 14px 12px;
  white-space: pre-line;
  border: 1px solid #67d1d0;
  border-radius: 14px;
  background: #e9f8f7;
  color: var(--black-900);
  font-family: "BM Kkubulim", sans-serif;
  font-size: 11px;
  line-height: 1.45;
}
.guide-character--compact {
  right: max(12px, calc((100% - 402px) / 2 + 12px));
  left: auto;
  width: auto;
  gap: 5px;
  padding: 6px;
  transform: none;
}
.guide-character--compact div {
  width: 48px;
  height: 44px;
  flex-basis: 48px;
}
.guide-character--compact p {
  flex: none;
  margin: 0;
  padding: 7px 9px;
  border-radius: 10px;
  font-family: inherit;
  font-size: 11px;
  font-weight: 700;
}
</style>
