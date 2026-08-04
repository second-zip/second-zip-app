const VALID_TONES = new Set(["blue", "purple", "pink", "green"]);

// 카드 데이터를 안전한 화면 출력 형식으로 변환합니다.
export const normalizeDictionaryCards = (cards = []) =>
  cards.filter(Boolean).map((card, index) => ({
    id: String(card.id ?? `dictionary-${index + 1}`),
    title: String(card.title ?? ""),
    description: String(card.description ?? ""),
    imageKey: String(card.imageKey ?? ""),
    tone: VALID_TONES.has(card.tone) ? card.tone : "blue",
    routeName: String(card.routeName ?? ""),
  }));
