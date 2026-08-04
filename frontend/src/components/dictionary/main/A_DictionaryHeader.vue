<script setup>
import dictionaryIcon from "@/assets/icons/nav/dict-blue-22.svg";
defineProps({
  title: { type: String, required: true },
  description: { type: String, required: true },
});
</script>
<template>
  <header class="dictionary-header">
    <div class="dictionary-header__title-row">
      <h1>{{ title }}</h1>
      <img :src="dictionaryIcon" alt="" />
    </div>
    <p>{{ description }}</p>
  </header>
</template>
<style scoped>
.dictionary-header {
  display: flex;
  min-height: 170px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--blue-100);
  color: var(--black-900);
}
.dictionary-header__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0;
}
img {
  width: 24px;
  height: 24px;
}
p {
  margin: 8px 0 0;
  color: var(--black-500);
  font-size: 12px;
}
</style>
