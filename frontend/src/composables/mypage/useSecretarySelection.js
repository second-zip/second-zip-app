import { nextTick, onMounted, ref } from 'vue';

import { getApiError } from '@/api/utils/error';
import { SECRETARY_OPTIONS } from '@/constants/mypage';
import { useAuthStore } from '@/stores/auth';
import { withObjectParticle } from '@/utils/mypage';

const DEFAULT_CHARACTER = 'CAT';
const BOUNCE_DURATION_MS = 320;

export const useSecretarySelection = () => {
  const authStore = useAuthStore();
  const selectedCharacter = ref(authStore.characterType ?? DEFAULT_CHARACTER);
  const animatingCharacter = ref(null);
  const saving = ref(false);
  const message = ref('');

  onMounted(async () => {
    if (!authStore.isAuthenticated || authStore.myPage) return;
    const account = await authStore.fetchMyPage();
    selectedCharacter.value = account.characterType;
  });

  const playBounce = async (characterType) => {
    // 같은 카드를 연속 클릭해도 CSS 애니메이션이 다시 시작되도록 class를 한 프레임 제거합니다.
    animatingCharacter.value = null;
    await nextTick();
    animatingCharacter.value = characterType;
    window.setTimeout(() => {
      if (animatingCharacter.value === characterType) animatingCharacter.value = null;
    }, BOUNCE_DURATION_MS);
  };

  const makeSelectionMessage = (characterType) => {
    const label = SECRETARY_OPTIONS.find(({ value }) => value === characterType)?.label ?? 'AI 비서';
    return `${withObjectParticle(label)} 비서로 선택하셨습니다. 변경 전까지, 위 비서로 적용됩니다.`;
  };

  const selectSecretary = async (characterType) => {
    if (saving.value) return;

    playBounce(characterType);
    const previousCharacter = selectedCharacter.value;
    selectedCharacter.value = characterType;

    if (characterType === authStore.myPage?.characterType) {
      message.value = makeSelectionMessage(characterType);
      return;
    }

    saving.value = true;
    message.value = '';
    try {
      await authStore.changeCharacter(characterType);
      message.value = makeSelectionMessage(characterType);
    } catch (error) {
      // 서버 저장에 실패하면 UI도 실제 계정 설정과 동일한 이전 값으로 되돌립니다.
      selectedCharacter.value = previousCharacter;
      message.value = getApiError(error).message;
    } finally {
      saving.value = false;
    }
  };

  const showPreparingMessage = () => {
    message.value = '비서가 준비 중이니 기대해 주세요!';
  };

  return {
    animatingCharacter,
    message,
    saving,
    selectedCharacter,
    selectSecretary,
    showPreparingMessage,
  };
};
