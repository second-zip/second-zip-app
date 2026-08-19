<script setup>
defineProps({ item: { type: Object, required: true } });
</script>
<template>
  <li class="word-item">
    <h2><span aria-hidden="true"></span>{{ item.term }}</h2>
    <p>{{ item.description }}</p>
  </li>
</template>
<style scoped>
.word-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  color: var(--black-900);
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0;
}
h2 span {
  width: 8px;
  height: 8px;
  flex: 0 0 8px;
  border-radius: 50%;
  background: var(--blue-900);
}
p {
  min-height: 28px;
  margin: 0;
  padding: 6px 14px;
  border: 1px solid var(--blue-300);
  border-radius: 14px;
  background: var(--blue-100);
  color: var(--black-700);
  font-size: 12px;
  line-height: 1.35;
}
</style>
