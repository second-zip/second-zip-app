const VALID_TONES = new Set(["blue", "purple", "pink", "green"]);

export const normalizeDictionaryCards = (cards = []) =>
  cards.filter(Boolean).map((card, index) => ({
    id: String(card.id ?? `dictionary-${index + 1}`),
    title: String(card.title ?? ""),
    description: String(card.description ?? ""),
    image: card.image ?? "",
    tone: VALID_TONES.has(card.tone) ? card.tone : "blue",
    routeName: String(card.routeName ?? ""),
  }));
