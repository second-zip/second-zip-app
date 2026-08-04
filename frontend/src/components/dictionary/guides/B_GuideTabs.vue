<script setup>
defineProps({
  tabs: { type: Array, required: true },
  modelValue: { type: String, required: true },
});
defineEmits(["update:modelValue"]);
</script>
<template>
  <div class="guide-tabs" role="tablist" aria-label="가이드 종류">
    <button
      v-for="tab in tabs"
      :id="`${tab.id}-tab`"
      :key="tab.id"
      type="button"
      role="tab"
      :aria-selected="modelValue === tab.id"
      :aria-controls="`${tab.id}-panel`"
      :class="{ 'is-active': modelValue === tab.id }"
      @click="$emit('update:modelValue', tab.id)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>
<style scoped>
.guide-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
  margin: 4px 17px 0;
  padding: 4px;
  border: 1px solid var(--black-100);
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 2px 7px rgb(17 17 24 / 7%);
}
button {
  min-width: 0;
  height: 40px;
  overflow: hidden;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--black-300);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0;
  white-space: nowrap;
  text-overflow: ellipsis;
}
button.is-active {
  background: var(--blue-900);
  color: #fff;
  box-shadow: 0 4px 8px rgb(7 94 241 / 24%);
}
button:focus-visible {
  outline: 3px solid var(--blue-300);
  outline-offset: 1px;
}
</style>
