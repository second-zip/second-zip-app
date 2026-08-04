<script setup>
import DictionaryMenuCard from "./DictionaryMenuCard.vue";
defineProps({ card: { type: Object, required: true } });
defineEmits(["select"]);
</script>
<template>
  <DictionaryMenuCard :card="card" @select="$emit('select', $event)" />
</template>
