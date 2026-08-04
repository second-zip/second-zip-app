<script setup>
import { getReports } from '@/api/report';
import { onMounted, ref } from 'vue';

import { getApiError } from '@/api/utils/error';

const reports = ref([]);
const errorMessage = ref('');

const fetchReports = async () => {
  const data = await getReports();
  reports.value = data.reports;
};

onMounted(async () => {
  try {
    await fetchReports();
  } catch (error) {
    errorMessage.value = getApiError(error).message;
  }
});
</script>

<template>
  <div>{{ reports.length }}</div>
</template>

<style scoped></style>
