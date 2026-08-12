<script setup>
import LogoImage from '@/assets/images/main-logo.png';
</script>

<template>
  <header
    class="main-hero w-100 d-flex align-items-center justify-content-center"
  >
    <img
      :src="LogoImage"
      class="main-hero__logo"
      alt="이번집 전세사기 유형별 방어 시스템"
    />
  </header>
</template>

<style scoped>
.main-hero {
  min-height: 136px;
  padding: 24px 0 16px;
}

.main-hero__logo {
  display: block;
  width: min(82%, 290px);
  height: auto;
  object-fit: contain;
}
</style>
