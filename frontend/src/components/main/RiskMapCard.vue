<template>
  <section class="risk-map-card w-100">
    <div class="risk-map-card__stage w-100">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.risk-map-card {
  padding: 16px 12px 14px;
  background-color: #fff;
  border: 1px solid var(--black-100);
  border-radius: 18px;
  box-shadow: 0 3px 12px rgb(17 17 24 / 6%);
}

.risk-map-card__stage {
  min-height: 260px;
}

</style>
