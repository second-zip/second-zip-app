<!-- 위험도 등급과 비서 캐릭터 메시지를 보여주는 첫 번째 분석 구역입니다. -->
<script setup>
defineProps({
  risk: { type: String, required: true },
  character: { type: String, required: true },
  icon: { type: String, required: true },
  message: { type: String, required: true },
});
</script>

<template>
  <section class="risk-summary d-flex" :class="`risk-summary--${risk}`">
    <div
      class="character-wrap d-flex align-items-center justify-content-center rounded-circle"
      :class="`character-wrap--${risk}`"
    >
      <img :src="character" alt="" />
    </div>
    <div class="risk-copy d-flex flex-column justify-content-center">
      <p class="font-kkubulim mb-0">이번 집의 전세 위험도는...</p>
      <div class="risk-result d-flex align-items-center justify-content-center">
        <img :src="icon" alt="" />
        <strong class="font-kkubulim">{{ message }}</strong>
      </div>
    </div>
  </section>
</template>

<style scoped>
.risk-summary {
  position: relative;
  width: 100%;
  height: 6.25rem;
  overflow: visible !important;
}

.risk-copy {
  min-width: 0;
  flex: 1;
  height: 6.25rem;
  margin-left: 7.0875rem;
  padding: 0.625rem 0.75rem;
  background: var(--green-100);
  border: 0.0625rem solid var(--green-500);
  border-radius: 1.25rem;
}

.risk-summary--caution .risk-copy {
  background: var(--yellow-100);
  border-color: var(--yellow-500);
}

.risk-summary--danger .risk-copy {
  background: var(--red-100);
  border-color: var(--red-500);
}

.character-wrap {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 2;
  width: 6.25rem;
  height: 6.25rem;
  overflow: hidden;
  background: var(--green-500);
  border: 0.0625rem solid var(--green-500);
}

.character-wrap--caution {
  background: var(--yellow-500);
  border-color: var(--yellow-500);
}

.character-wrap--danger {
  background: var(--red-500);
  border-color: var(--red-500);
}

.character-wrap img {
  width: 6.25rem;
  height: 6.25rem;
  object-fit: contain;
}

.risk-copy p {
  font-size: 0.75rem;
}

.risk-result {
  gap: 0.5rem;
  margin-top: 0.375rem;
}

.risk-result img {
  width: 3.25rem;
  height: 3.25rem;
}

.risk-result strong {
  min-width: 0;
  font-size: clamp(0.875rem, 4vw, 1.25rem);
  line-height: 1.2;
  text-align: center;
  overflow-wrap: anywhere;
}
</style>
