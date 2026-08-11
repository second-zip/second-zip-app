<script setup>
import PlusIcon from '@/assets/icons/report/plus-white-14.svg';
import ReportImage from '@/assets/images/report-banner.png';
</script>

<template>
  <section class="report-create-box position-relative w-100 overflow-hidden">
    <div
      class="report-create-box__content position-relative h-100 d-flex flex-column justify-content-center"
    >
      <p class="report-create-box__eyebrow fw-semibold mb-2">
        이 집, 보증금 돌려받을 수 있을까?
      </p>

      <h2 class="report-create-box__title fw-bold mb-2">
        <span>리포트</span>를 만들어 보세요
      </h2>

      <p class="report-create-box__description fw-medium mb-3">
        위험도를 분석하여<br />
        안전한 선택을 도와드려요.
      </p>

      <RouterLink
        to="/reports/create"
        class="report-create-box__button d-flex align-items-center justify-content-center fw-semibold text-decoration-none"
      >
        <img :src="PlusIcon" class="report-create-box__button-icon" alt="" />
        <span>리포트 생성하기</span>
      </RouterLink>
    </div>

    <img
      :src="ReportImage"
      class="report-create-box__image position-absolute"
      alt=""
    />
  </section>
</template>

<style scoped>
.report-create-box {
  flex-shrink: 0;
  height: 200px;
  padding: 16px 24px;
  background-color: var(--blue-100);
  border-radius: 24px;
}

.report-create-box__content {
  z-index: 1;
  width: calc(100% - 110px);
}

.report-create-box__eyebrow {
  color: var(--blue-700);
  font-size: 0.75rem;
  line-height: 1.4;
}

.report-create-box__title {
  color: var(--black-900);
  font-size: 1.25rem;
  line-height: 1.3;
  letter-spacing: -0.04em;
}

.report-create-box__title span {
  color: var(--blue-900);
}

.report-create-box__description {
  color: var(--black-500);
  font-size: 0.75rem;
  line-height: 1.7;
}

.report-create-box__button {
  width: fit-content;
  height: 34px;
  padding: 0 18px;
  color: white;
  font-size: 0.75rem;
  background-color: var(--blue-900);
  border-radius: 14px;
  gap: 6px;
}

.report-create-box__button-icon {
  width: 14px;
  height: 14px;
}

.report-create-box__image {
  top: 50%;
  right: 12px;
  width: 100px;
  height: auto;
  transform: translateY(-50%);
  object-fit: contain;
}
</style>
