<script setup>
import CFraudTypeCard from "./C_FraudTypeCard.vue";
defineProps({ types: { type: Array, required: true } });
defineEmits(["play"]);
</script>
<template>
  <div class="fraud-list">
    <CFraudTypeCard
      v-for="type in types"
      :key="type.id"
      :type="type"
      @play="$emit('play', $event)"
    />
  </div>
</template>
<style scoped>
.fraud-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 23px 130px;
}
</style>
