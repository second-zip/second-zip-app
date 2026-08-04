<script setup>
const riskLevels = [
  {
    key: 'very-dangerous',
    label: '매우위험',
  },
  {
    key: 'dangerous',
    label: '위험',
  },
  {
    key: 'very-caution',
    label: '매우주의',
  },
  {
    key: 'caution',
    label: '주의',
  },
  {
    key: 'safe',
    label: '안전',
  },
  {
    key: 'very-safe',
    label: '매우안전',
  },
];
</script>

<template>
  <section class="risk-map-card w-100">
    <div class="risk-map-card__stage w-100">
      <slot />
    </div>
    <div
      class="risk-map-card__legend d-flex align-items-center justify-content-center flex-wrap"
    >
      <div
        v-for="level in riskLevels"
        :key="level.key"
        class="risk-map-card__legend-item d-flex align-items-center gap-1"
      >
        <span
          class="risk-map-card__legend-color"
          :class="`risk-map-card__legend-color--${level.key}`"
        />

        <span class="risk-map-card__legend-label fw-semibold">
          {{ level.label }}
        </span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.risk-map-card {
  padding: 12px 16px;
  background-color: #fff;
  border: 1px solid var(--black-100);
  border-radius: 18px;
  box-shadow: 0 3px 12px rgb(17 17 24 / 6%);
}

.risk-map-card__stage {
  min-height: 260px;
}

.risk-map-card__legend {
  gap: 7px 9px;
  min-height: 28px;
}

.risk-map-card__legend-item {
  flex-shrink: 0;
}

.risk-map-card__legend-color {
  width: 11px;
  height: 11px;
  flex-shrink: 0;
  border-radius: 3px;
}

.risk-map-card__legend-label {
  color: var(--black-500);
  font-size: 0.6875rem;
  white-space: nowrap;
}

.risk-map-card__legend-color--very-dangerous {
  background-color: #f20d17;
  border: 1px solid #d90009;
}

.risk-map-card__legend-color--dangerous {
  background-color: #ffc4c6;
  border: 1px solid #ff2a32;
}

.risk-map-card__legend-color--very-caution {
  background-color: #f39a10;
  border: 1px solid #d77e00;
}

.risk-map-card__legend-color--caution {
  background-color: #fff0c9;
  border: 1px solid #f4a000;
}

.risk-map-card__legend-color--safe {
  background-color: #c7f4df;
  border: 1px solid #13bd75;
}

.risk-map-card__legend-color--very-safe {
  background-color: #18aa68;
  border: 1px solid #078c50;
}
</style>
