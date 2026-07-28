<script setup>
defineProps({
  titleRatio: {
    type: Number,
    default: 30,
    validator: (value) => value >= 0 && value <= 100,
  },
});
</script>

<template>
  <div class="sheet-layout w-100 min-vh-100 d-flex flex-column">
    <header
      class="w-100 flex-shrink-0 d-flex align-items-center"
      :style="{ minHeight: `${titleRatio}dvh` }"
    >
      <slot name="header" />
    </header>

    <section class="bottom-sheet w-100 flex-grow-1">
      <slot />
    </section>
  </div>
</template>

<style scoped>
.sheet-layout {
  min-height: 100dvh;
  background-color: var(--blue-100);
}

.bottom-sheet {
  min-height: 0;
  padding-bottom: 64px;
  background-color: white;
  border-radius: 36px 36px 0 0;
  box-shadow: 0 0 32px rgb(17 17 24 / 6%);
}
</style>
