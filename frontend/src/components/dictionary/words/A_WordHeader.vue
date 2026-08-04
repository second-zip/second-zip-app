<script setup>
import dictionaryIcon from "@/assets/icons/nav/dict-blue-22.svg";
defineProps({
  title: { type: String, required: true },
  sectionTitle: { type: String, required: true },
});
</script>
<template>
  <header class="word-header">
    <RouterLink to="/dictionary" class="word-header__brand">
      <strong>{{ title }}</strong
      ><img :src="dictionaryIcon" alt="" />
    </RouterLink>
    <h1>{{ sectionTitle }}</h1>
  </header>
</template>
<style scoped>
.word-header {
  display: flex;
  height: 68px;
  align-items: center;
  justify-content: space-between;
  padding: 0 17px;
  background: var(--blue-100);
}
.word-header__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--black-900);
  text-decoration: none;
}
.word-header__brand strong {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0;
}
.word-header__brand img {
  width: 22px;
  height: 22px;
}
h1 {
  margin: 0;
  color: var(--black-500);
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0;
}
</style>
