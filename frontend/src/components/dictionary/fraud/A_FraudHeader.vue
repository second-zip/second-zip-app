<script setup>
import dictionaryIcon from "@/assets/icons/nav/dict-blue-22.svg";
defineProps({
  title: { type: String, required: true },
  currentTitle: { type: String, required: true },
  backTo: { type: String, default: "/dictionary" },
});
</script>
<template>
  <header class="fraud-header">
    <RouterLink :to="backTo"
      ><strong>{{ title }}</strong
      ><img :src="dictionaryIcon" alt=""
    /></RouterLink>
    <h1>{{ currentTitle }}</h1>
  </header>
</template>
<style scoped>
.fraud-header {
  display: flex;
  height: 68px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 20px;
  background: var(--blue-100);
}
a {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
  color: var(--black-900);
  text-decoration: none;
}
strong {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0;
}
img {
  width: 22px;
  height: 22px;
}
h1 {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: var(--black-500);
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
