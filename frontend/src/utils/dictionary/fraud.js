export const normalizeFraudTypes = (types = []) =>
  types.filter(Boolean).map((type, index) => ({
    id: String(type.id ?? `fraud-${index + 1}`),
    number: Number(type.number) || index + 1,
    title: String(type.title ?? ""),
    hashtags: (type.hashtags ?? [])
      .filter(Boolean)
      .map((hashtag) => String(hashtag).replace(/^#/, "").trim())
      .filter(Boolean),
    description: String(type.description ?? ""),
    videoSrc: String(type.videoSrc ?? ""),
  }));
