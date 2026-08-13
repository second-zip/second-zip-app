<template>
  <div
    class="recording-processing d-flex align-items-center justify-content-center gap-2"
    role="status"
  >
    <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
    <span class="fw-semibold">녹음 내용을 분석하고 있어요.</span>
  </div>
</template>

<style scoped>
.recording-processing {
  min-height: 32px;
  color: var(--black-500);
  font-size: 0.75rem;
}
.recording-processing .spinner-border { color: var(--mint-500); }
</style>
