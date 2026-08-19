<script setup>
defineProps({
  image: { type: String, required: true },
  message: { type: String, required: true },
});
</script>
<template>
  <aside class="dictionary-guide">
    <div class="dictionary-guide__character"><img :src="image" alt="" /></div>
    <p>{{ message }}</p>
  </aside>
</template>
<style scoped>
.dictionary-guide {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 24px 12px 22px;
}
.dictionary-guide__character {
  width: 96px;
  height: 86px;
  flex: 0 0 96px;
  overflow: hidden;
  border-radius: 50%;
  background: #78d8d5;
}
.dictionary-guide__character img {
  width: 100%;
  height: 112%;
  object-fit: contain;
  object-position: center top;
}
p {
  position: relative;
  flex: 1;
  margin: 0;
  padding: 15px 12px;
  white-space: pre-line;
  border: 1px solid #67d1d0;
  border-radius: 14px;
  background: #e9f8f7;
  color: var(--black-900);
  font-family: "BM Kkubulim", sans-serif;
  font-size: 11px;
  line-height: 1.45;
}
p::before {
  position: absolute;
  top: 26px;
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
