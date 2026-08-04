export const DEFAULT_CHARACTER = 'cat';

const CHARACTER_TYPES = new Set(['cat', 'man', 'woman']);

export const normalizeCharacterType = (
  characterType,
  fallbackCharacter = DEFAULT_CHARACTER,
) => {
  const normalized = String(characterType ?? '').trim().toLowerCase();

  return CHARACTER_TYPES.has(normalized) ? normalized : fallbackCharacter;
};

