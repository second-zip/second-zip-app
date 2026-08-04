<script setup>
defineProps({
  image: { type: String, required: true },
  message: { type: String, required: true },
});
</script>
<template>
  <aside>
    <div><img :src="image" alt="" /></div>
    <p>{{ message }}</p>
  </aside>
</template>
<style scoped>
aside {
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
div {
  width: 94px;
  height: 85px;
  flex: 0 0 94px;
  overflow: hidden;
  border-radius: 50%;
  background: #78d8d5;
}
img {
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
</style>
