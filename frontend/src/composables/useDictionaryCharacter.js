import { computed, onMounted } from "vue";

import { DICTIONARY_CHARACTERS } from "@/constants/dictionary/characters";
import { useAuthStore } from "@/stores/auth";
import { resolveDictionaryCharacter } from "@/utils/dictionary/characters";

export const useDictionaryCharacter = () => {
  const authStore = useAuthStore();

  // 비로그인은 고양이, 로그인 사용자는 프로필의 비서 설정을 사용합니다.
  const characterKey = computed(() =>
    resolveDictionaryCharacter(
      authStore.isAuthenticated,
      authStore.myPage?.characterType,
    ),
  );
  const character = computed(
    () => DICTIONARY_CHARACTERS[characterKey.value],
  );

  // 새로고침으로 프로필 정보가 비어 있으면 서버에서 최신 설정을 가져옵니다.
  onMounted(async () => {
    if (!authStore.isAuthenticated || authStore.myPage?.characterType) return;

    try {
      await authStore.fetchMyPage();
    } catch {
      // 조회 실패 시 캐릭터 판정 로직이 고양이 기본값을 사용합니다.
    }
  });

  return {
    character,
    characterKey,
  };
};
