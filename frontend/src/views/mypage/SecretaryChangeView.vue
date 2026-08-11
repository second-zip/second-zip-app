<script setup>
import SecretaryCard from '@/components/mypage/SecretaryCard.vue';
import { useSecretarySelection } from '@/composables/mypage/useSecretarySelection';
import { SECRETARY_OPTIONS } from '@/constants/mypage';

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
  <div class="secretary-page">
    <header class="secretary-page__header text-center">
      <h1>AI 비서 변경하기</h1>
      <p>나의 맞춤형 비서는 누구?</p>
    </header>
    <main class="secretary-page__sheet">
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
  </div>
</template>

<style scoped>
.secretary-page { display: flex; height: 100%; min-height: 0; flex-direction: column; overflow: hidden; padding-top: 1px; background: #f1f5fd; color: #111827; }
.secretary-page__header { flex: 0 0 84px; padding: 20px 20px 13px; }
.secretary-page__header h1 { margin: 0 0 7px; font-size: 20px; font-weight: 800; }
.secretary-page__header p { margin: 0; color: #777f8e; font-size: 9px; }
.secretary-page__sheet { position: relative; min-height: 0; flex: 1; padding: 13px 13px 14px; border-radius: 27px 27px 0 0; background: #fff; }
.secretary-grid { display: grid; width: 100%; height: 100%; grid-template-columns: repeat(2, minmax(0, 1fr)); grid-template-rows: repeat(2, minmax(0, 1fr)); gap: 10px; }
.secretary-page__message { position: absolute; right: 18px; bottom: 16px; left: 18px; z-index: 2; margin: 0; padding: 7px; color: #176cf3; border-radius: 8px; background: rgba(255,255,255,.94); text-align: center; font-size: 10px; box-shadow: 0 2px 8px rgba(24,48,88,.15); }

@media (max-height: 600px) {
  .secretary-page__header { flex-basis: 70px; padding-top: 14px; }
  .secretary-page__header h1 { font-size: 18px; }
  .secretary-page__sheet { padding: 10px 12px; }
  .secretary-grid { gap: 8px; }
}
</style>
