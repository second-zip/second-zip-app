<script setup>
import { computed, onMounted, ref } from 'vue';

import { useAuthStore } from '@/stores/auth';
import { logger } from '@/utils/logger';
import SecretaryGuide from './SecretaryGuide.vue';

defineOptions({ inheritAttrs: false });
const CHARACTER_TYPES = new Set(['CAT', 'MAN', 'WOMAN']);
const props = defineProps({
  messages: { type: Object, required: true },
  floating: { type: Boolean, default: false },
  changeBtn: { type: Boolean, default: false },
});
const emit = defineEmits(['change']);
const authStore = useAuthStore();
const isLoading = ref(
  authStore.isAuthenticated && !authStore.myPage?.characterType,
);
const characterType = computed(() => {
  const type = authStore.myPage?.characterType ?? authStore.characterType;
  return CHARACTER_TYPES.has(type) ? type : 'CAT';
});
const text = computed(
  () => props.messages[characterType.value] ?? props.messages.CAT ?? '',
);

onMounted(async () => {
  if (!isLoading.value) return;
  try {
    await authStore.fetchMyPage();
  } catch (error) {
    logger.error('member-secretary.fetch-user', error);
  } finally {
    isLoading.value = false;
  }
});
</script>

<template>
  <SecretaryGuide
    v-if="!isLoading"
    v-bind="$attrs"
    :text="text"
    :floating="floating"
    :change-btn="changeBtn"
    :character-type="characterType"
    @change="emit('change')"
  />
</template>
