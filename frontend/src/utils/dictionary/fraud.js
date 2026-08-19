const YOUTUBE_EMBED_PATTERN =
  /^https:\/\/(?:www\.)?youtube(?:-nocookie)?\.com\/embed\/([^?&/]+)/;
const YOUTUBE_THUMBNAIL_BASE_URL = "https://i.ytimg.com/vi";

// 임베드 URL에서 유튜브 영상 ID를 추출합니다.
const getYouTubeVideoId = (videoSrc = "") =>
  String(videoSrc).match(YOUTUBE_EMBED_PATTERN)?.[1] ?? "";

// 영상 주소가 지원하는 유튜브 임베드 형식인지 판별합니다.
export const isYouTubeEmbedUrl = (videoSrc = "") =>
  YOUTUBE_EMBED_PATTERN.test(String(videoSrc));

// 유튜브 영상 ID와 화질에 맞는 썸네일 주소를 생성합니다.
const createYouTubeThumbnailUrl = (videoId, quality) =>
  videoId ? `${YOUTUBE_THUMBNAIL_BASE_URL}/${videoId}/${quality}.jpg` : "";

export const normalizeFraudTypes = (types = []) =>
  types.filter(Boolean).map((type, index) => {
    const videoSrc = String(type.videoSrc ?? "");
    const videoId = getYouTubeVideoId(videoSrc);

    return {
      id: String(type.id ?? `fraud-${index + 1}`),
      number: Number(type.number) || index + 1,
      title: String(type.title ?? ""),
      hashtags: (type.hashtags ?? [])
        .filter(Boolean)
        .map((hashtag) => String(hashtag).replace(/^#/, "").trim())
        .filter(Boolean),
      description: String(type.description ?? ""),
      videoSrc,
      // 유튜브 영상은 고해상도 썸네일과 실패 시 사용할 대체 이미지를 제공합니다.
      thumbnailSrc:
        createYouTubeThumbnailUrl(videoId, "maxresdefault") ||
        String(type.thumbnailSrc ?? ""),
      thumbnailFallbackSrc: createYouTubeThumbnailUrl(videoId, "hqdefault"),
    };
  });
