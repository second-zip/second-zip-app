import {
  DEFAULT_DICTIONARY_CHARACTER,
  DICTIONARY_CHARACTERS,
} from "@/constants/dictionary/characters";
import { normalizeCharacterType } from "@/utils/character";

// 서버 캐릭터 값을 도감에서 지원하는 키로 정규화합니다.
export const normalizeDictionaryCharacter = (characterType) =>
  normalizeCharacterType(characterType, DEFAULT_DICTIONARY_CHARACTER);

export const resolveDictionaryCharacter = (
  isAuthenticated,
  characterType,
) => {
  // 비로그인 사용자의 프로필 데이터는 사용하지 않습니다.
  if (!isAuthenticated) return DEFAULT_DICTIONARY_CHARACTER;

  return normalizeDictionaryCharacter(characterType);
};
