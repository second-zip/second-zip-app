<script setup>
import CWordItem from "./C_WordItem.vue";
defineProps({ items: { type: Array, required: true } });
</script>
<template>
  <ul class="word-list">
    <CWordItem v-for="item in items" :key="item.id" :item="item" />
  </ul>
</template>
<style scoped>
.word-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin: 0;
  padding: 24px 17px 130px;
  list-style: none;
}
</style>
