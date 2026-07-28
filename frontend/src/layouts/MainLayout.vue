<script setup>
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import { RouterView } from 'vue-router';
</script>

<template>
  <div class="app-layout min-vh-100">
    <main class="main-content min-vh-100 bg-white overflow-x-hidden mx-auto">
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
  min-height: 100dvh;
}

.app-layout {
  width: 100%;
  background-color: #fff;
}

.main-content {
  width: 100%;
  padding-bottom: 64px;
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
