<!-- AI 추천 특약과 안내 문구를 보여주는 다섯 번째 분석 구역입니다. -->
<script setup>
defineProps({
  terms: { type: Array, required: true },
  notice: { type: String, required: true },
  character: { type: String, required: true },
});
</script>

<template>
  <section class="special-terms">
    <div class="special-terms-heading d-flex align-items-center">
      <span class="heading-dot" aria-hidden="true"></span>
      <h2 class="mb-0">AI가 추천하는 특약 사항</h2>
    </div>

    <ol class="special-terms-list">
      <li v-for="(term, index) in terms" :key="term.title" class="special-term">
        <div class="term-title d-flex align-items-center">
          <span class="term-number">{{ index + 1 }}</span>
          <strong>{{ term.title }}</strong>
        </div>
        <p>{{ term.content }}</p>
      </li>
    </ol>

    <p class="special-terms-notice mb-0">{{ notice }}</p>
    <img class="special-terms-character" :src="character" alt="" />
  </section>
</template>

<style scoped>
.special-terms {
  position: relative;
  margin-top: 1rem;
  padding: 1rem 1rem 4rem;
  overflow: visible;
  background: white;
  border: 0.0625rem dashed var(--black-300);
  border-radius: 1rem;
}

.special-terms-heading {
  gap: 0.5rem;
  padding-bottom: 0.875rem;
  border-bottom: 0.0625rem dashed var(--black-100);
}

.special-terms-heading h2 {
  font-size: 0.875rem;
  font-weight: 700;
}

.heading-dot {
  width: 0.375rem;
  height: 0.375rem;
  background: var(--blue-900);
  border-radius: 50%;
}

.special-terms-list {
  position: relative;
  z-index: 3;
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  margin: 1rem 0 0;
  padding: 0 0.375rem 0.75rem 0;
  background: white;
  border-bottom: 0.0625rem dashed var(--black-100);
  list-style: none;
}

.special-term {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.term-title {
  gap: 0.5rem;
}

.term-title strong {
  font-size: 0.75rem;
  font-weight: 700;
}

.term-number {
  display: inline-flex;
  width: 1.25rem;
  height: 1.25rem;
  align-items: center;
  justify-content: center;
  flex: 0 0 1.25rem;
  color: var(--blue-900);
  background: var(--blue-100);
  border: 0.0625rem solid var(--blue-300);
  border-radius: 50%;
  font-size: 0.625rem;
  font-weight: 700;
}

.special-term p {
  margin: 0 0 0 1.75rem;
  padding: 0.75rem;
  color: var(--black-700);
  background: var(--blue-100);
  border: 0.0625rem solid var(--blue-300);
  border-radius: 0.75rem;
  font-size: 0.625rem;
  line-height: 1.5;
  box-shadow: 0 0.125rem 0.375rem rgb(59 128 255 / 10%);
}

.special-terms-notice {
  position: absolute;
  right: 1rem;
  bottom: 0.875rem;
  left: 1rem;
  z-index: 3;
  max-width: 13rem;
  color: var(--black-500);
  font-size: 0.5625rem;
  line-height: 1.4;
  white-space: pre-line;
}

.special-terms-character {
  position: absolute;
  right: -4.5rem;
  bottom: -11rem;
  z-index: 2;
  width: 15rem;
  height: 15rem;
  object-fit: contain;
  pointer-events: none;
}
</style>
