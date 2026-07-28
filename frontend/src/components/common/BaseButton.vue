<template>
  <button
    type="button"
    class="base-button w-100 d-flex align-items-center justify-content-center flex-shrink-0 border-0 text-white fs-6 fw-semibold lh-1"
  >
    <slot />
  </button>
</template>

<style scoped>
.base-button {
  height: 56px;
  border-radius: 18px;
  background-color: var(--blue-900, #075ef1);
  box-shadow: 0 4px 12px rgb(7 94 241 / 35%);
  cursor: pointer;
}

.base-button:hover {
  filter: brightness(0.95);
}

.base-button:active {
  transform: translateY(1px);
}

.base-button:focus-visible {
  outline: 3px solid rgb(7 94 241 / 25%);
  outline-offset: 3px;
}
</style>
