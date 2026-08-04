export const normalizeWordItems = (items = []) =>
  items
    .filter((item) => Array.isArray(item) && item.length >= 2)
    .map(([term, description], index) => ({
      id: `word-${index + 1}`,
      term: String(term).trim(),
      description: String(description).trim(),
    }));
