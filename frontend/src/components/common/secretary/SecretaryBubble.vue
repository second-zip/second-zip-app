<script setup>
defineProps({
  text: {
    type: String,
    required: true,
  },
});
</script>

<template>
  <div
    class="secretary-bubble flex-grow-1 d-flex align-items-center"
    role="status"
  >
    <p class="font-kkubulim m-0">
      {{ text }}
    </p>
  </div>
</template>

<style scoped>
.secretary-bubble {
  min-width: 0;
  height: 72px;
  padding: 0 16px;

  color: var(--black-900);
  background-color: var(--mint-100);

  border: 1px solid var(--mint-500);
  border-radius: 4px 16px 16px 16px;

  align-self: flex-end;
}

.secretary-bubble p {
  width: 100%;
  overflow: hidden;

  font-size: 0.875rem;
  line-height: 1.4;

  display: -webkit-box;
  white-space: pre-line;
  -webkit-box-orient: vertical;
}
</style>
