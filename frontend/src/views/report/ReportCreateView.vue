<script setup>
import ReportCreateForm from '@/components/report/create/ReportCreateForm.vue';
</script>

<template>
  <div class="report-create-view d-flex flex-column">
    <div class="report-create-view__body flex-grow-1">
      <ReportCreateForm />
    </div>
  </div>
</template>

<style scoped>
.report-create-view {
  width: 100%;
  height: 100%;
  padding: 20px;
  overflow: hidden;
  box-sizing: border-box;
  background-color: #fff;
}

.report-create-view__header {
  height: 56px;
}

.report-create-view__header h1 {
  color: var(--black-300);
  font-size: 0.875rem;
}

.report-create-view__body {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: none;
}

.report-create-view__body::-webkit-scrollbar {
  display: none;
}
</style>
