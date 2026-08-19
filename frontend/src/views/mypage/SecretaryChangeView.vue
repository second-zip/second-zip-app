<script setup>
import SecretaryCard from '@/components/mypage/SecretaryCard.vue';
import { useSecretarySelection } from '@/composables/mypage/useSecretarySelection';
import { SECRETARY_OPTIONS } from '@/constants/mypage';
import BottomSheetLayout from '@/layouts/BottomSheetLayout.vue';
import DefaultSheetHeader from '@/layouts/DefaultSheetHeader.vue';

const {
  animatingCharacter,
  message,
  saving,
  selectedCharacter,
  selectSecretary,
  showPreparingMessage,
} = useSecretarySelection();
</script>

<template>
  <BottomSheetLayout class="secretary-page" :title-ratio="15">
    <template #header>
      <DefaultSheetHeader
        title="AI 비서 변경하기"
        subtitle="나의 맞춤형 비서는 누구?"
      />
    </template>

    <main class="secretary-page__sheet h-100 overflow-y-auto">
      <div class="secretary-grid">
        <SecretaryCard
          v-for="option in SECRETARY_OPTIONS"
          :key="option.value"
          :option="option"
          :selected="option.value === selectedCharacter"
          :bouncing="animatingCharacter === option.value"
          :disabled="saving"
          @select="selectSecretary(option.value)"
        />
        <SecretaryCard preparing @select="showPreparingMessage" />
      </div>
      <p v-if="message" class="secretary-page__message" role="status">{{ message }}</p>
    </main>
  </BottomSheetLayout>
</template>

<style scoped>
.secretary-page {
  height: 100%;
  min-height: 0;
  color: #111827;
}

.secretary-page__sheet {
  position: relative;
  min-height: 0;
  padding: 13px 13px 14px;
  border-radius: 36px 36px 0 0;
  overscroll-behavior: contain;
}

.secretary-grid {
  display: grid;
  width: 100%;
  min-height: 100%;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(220px, 1fr));
  gap: 10px;
}

.secretary-page__message { position: absolute; right: 18px; bottom: 16px; left: 18px; z-index: 2; margin: 0; padding: 7px; color: #176cf3; border-radius: 8px; background: rgba(255,255,255,.94); text-align: center; font-size: 10px; box-shadow: 0 2px 8px rgba(24,48,88,.15); }

@media (max-height: 600px) {
  .secretary-page__sheet { padding: 10px 12px; }
  .secretary-grid { gap: 8px; }
}

@media (min-width: 768px) {
  .secretary-page__sheet {
    width: min(100%, 1000px);
    margin: 0 auto;
    padding: 20px;
  }
}

@media (min-width: 1024px) {
  .secretary-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    grid-template-rows: minmax(320px, 520px);
    align-content: center;
    gap: 16px;
  }
}
</style>
