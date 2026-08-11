<script setup>
defineProps({
  gap: {
    type: Number,
    default: 0,
  },
  shadow: {
    type: Boolean,
    default: false,
  },
  flush: {
    type: Boolean,
    default: false,
  },
});
</script>

<template>
  <div
    class="container d-flex flex-column rounded-4"
    :class="{
      'container-shadow': shadow,
      'container-flush': flush,
    }"
    :style="{ gap: `${gap}px` }"
  >
    <slot />
  </div>
</template>

<style scoped>
.container {
  border: 1px solid var(--black-100);
}

.container-flush {
  padding: 0;
}

.container-shadow {
  box-shadow: 0 2px 8px rgb(17 17 24 / 6%);
}
</style>
