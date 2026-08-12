<script setup>
defineProps({ title: { type: String, required: true }, back: { type: Boolean, default: false } });
</script>

<template>
  <header class="mypage-header d-flex align-items-center justify-content-center position-relative">
    <button v-if="back" type="button" class="mypage-header__back position-absolute border-0 bg-transparent" aria-label="뒤로 가기" @click="$router.back()">‹</button>
    <h1 class="m-0 fw-bold">{{ title }}</h1>
  </header>
</template>

<style scoped>
.mypage-header { height: 78px; background: #edf3ff; }
.mypage-header h1 { color: #111827; font-size: 20px; }
.mypage-header__back { left: 18px; color: #111827; font-size: 34px; line-height: 1; }
</style>
