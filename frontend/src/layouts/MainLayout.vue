<script setup>
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import { RouterView } from 'vue-router';
</script>

<template>
  <div class="app-layout">
    <main class="main-content bg-white overflow-x-hidden mx-auto">
      <RouterView />
    </main>
    <BottomNavigation />
  </div>
</template>

<style>
html,
body,
#app {
  width: 100%;
  min-height: 100%;
  margin: 0;
}
</style>

<style scoped>
.app-layout,
.main-content {
  width: 100%;
  min-height: 100dvh;
}

.app-layout {
  background-color: #fff;
}

.main-content {
  height: 100dvh;
  padding-bottom: 64px;
  box-sizing: border-box;
}

/* 태블릿·데스크톱 */
@media (min-width: 768px) {
  .app-layout {
    background-color: var(--blue-100);
  }

  .main-content {
    width: 402px;
  }
}
</style>
