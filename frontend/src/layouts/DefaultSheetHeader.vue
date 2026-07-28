<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    default: '',
  },
  icon: {
    type: String,
    default: '',
  },
});
</script>

<template>
  <div
    class="w-100 h-100 d-flex flex-column align-items-center justify-content-center text-center px-3"
  >
    <div class="d-flex align-items-center justify-content-center gap-2">
      <h1 class="fs-4 fw-bold mb-0">
        {{ title }}
      </h1>

      <img v-if="icon" :src="icon" class="header-icon flex-shrink-0" alt="" />
    </div>

    <p v-if="subtitle" class="subtitle fw-normal mt-2 mb-0">
      {{ subtitle }}
    </p>
  </div>
</template>

<style scoped>
h1 {
  color: var(--black-900);
}

.subtitle {
  color: var(--black-500);
  font-size: 0.875rem;
}

.header-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
}
</style>
