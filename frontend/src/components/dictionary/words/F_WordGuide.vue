<script setup>
defineProps({
  image: { type: String, required: true },
  message: { type: String, required: true },
});
</script>
<template>
  <aside class="word-guide">
    <div class="word-guide__character"><img :src="image" alt="" /></div>
    <p>{{ message }}</p>
  </aside>
</template>
<style scoped>
.word-guide {
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
.word-guide__character {
  width: 94px;
  height: 85px;
  flex: 0 0 94px;
  overflow: hidden;
  border-radius: 50%;
  background: #78d8d5;
  filter: drop-shadow(0 4px 4px rgb(17 17 24 / 10%));
}
.word-guide__character img {
  width: 100%;
  height: 112%;
  object-fit: contain;
  object-position: center top;
}
p {
  position: relative;
  flex: 1;
  margin: 20px 0 0;
  padding: 14px 12px;
  white-space: pre-line;
  border: 1px solid #67d1d0;
  border-radius: 14px;
  background: #e9f8f7;
  color: var(--black-900);
  font-family: "BM Kkubulim", sans-serif;
  font-size: 11px;
  line-height: 1.45;
  box-shadow: 0 3px 8px rgb(17 17 24 / 5%);
}
p::before {
  position: absolute;
  top: 23px;
  left: -7px;
  width: 12px;
  height: 12px;
  content: "";
  transform: rotate(45deg);
  border-bottom: 1px solid #67d1d0;
  border-left: 1px solid #67d1d0;
  background: #e9f8f7;
}
</style>
