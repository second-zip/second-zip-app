<script setup>
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import { RouterView } from 'vue-router';
</script>

<template>
  <div class="app-layout">
    <BottomNavigation />
    <main class="main-content bg-white overflow-x-hidden mx-auto">
      <RouterView />
    </main>
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

:root {
  --app-sidebar-width: 0px;
  --app-content-max-width: 1200px;
}

@media (min-width: 768px) {
  :root {
    --app-sidebar-width: 208px;
  }

  body {
    background-color: var(--blue-100);
  }
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
    display: grid;
    max-width: calc(
      var(--app-sidebar-width) + var(--app-content-max-width)
    );
    grid-template-columns:
      var(--app-sidebar-width)
      minmax(0, var(--app-content-max-width));
    margin: 0 auto;
    background-color: var(--blue-100);
    box-shadow: 0 0 32px rgb(17 17 24 / 8%);
  }

  .main-content {
    width: 100%;
    height: 100dvh;
    padding-bottom: 0;
    overflow-y: auto;
  }
}
</style>
