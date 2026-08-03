import { COMIC_VIEWER_OPTIONS } from "@/constants/dictionary/guides";

export const normalizeGuideConfig = (config = {}) => ({
  pageTitle: String(config.pageTitle ?? ""),
  headerTitle: String(config.headerTitle ?? ""),
  guideImage: config.guideImage ?? "",
  guideMessage: String(config.guideMessage ?? ""),
  tabs: (config.tabs ?? []).map((tab, index) => ({
    id: String(tab.id ?? `guide-tab-${index + 1}`),
    label: String(tab.label ?? ""),
    images: (tab.images ?? []).filter(Boolean).map((image, imageIndex) => ({
      id: image.id ?? `${tab.id}-image-${imageIndex + 1}`,
      src: typeof image === "string" ? image : image.src,
      alt: typeof image === "string" ? "" : String(image.alt ?? ""),
    })),
  })),
});

const contentRatioCache = new Map();

export const findLastContentRow = (
  pixels,
  width,
  height,
  options = COMIC_VIEWER_OPTIONS,
) => {
  const minimumMarks = Math.max(
    3,
    Math.floor(width * options.minimumMarkedPixelRatio),
  );

  for (let y = height - 1; y >= 0; y -= 1) {
    let markedPixels = 0;

    for (let x = 0; x < width; x += 1) {
      const offset = (y * width + x) * 4;
      const isMarked =
        pixels[offset] < options.whiteThreshold ||
        pixels[offset + 1] < options.whiteThreshold ||
        pixels[offset + 2] < options.whiteThreshold;

      if (!isMarked) continue;

      markedPixels += 1;
      if (markedPixels >= minimumMarks) return y;
    }
  }

  return height - 1;
};

export const findImageContentRatio = (image) => {
  if (contentRatioCache.has(image.src)) return contentRatioCache.get(image.src);

  const sampleWidth = Math.min(
    image.naturalWidth,
    COMIC_VIEWER_OPTIONS.contentSampleWidth,
  );
  const sampleHeight = Math.round(
    image.naturalHeight * (sampleWidth / image.naturalWidth),
  );
  const canvas = document.createElement("canvas");
  const context = canvas.getContext("2d", { willReadFrequently: true });

  canvas.width = sampleWidth;
  canvas.height = sampleHeight;
  context.drawImage(image, 0, 0, sampleWidth, sampleHeight);

  const pixels = context.getImageData(0, 0, sampleWidth, sampleHeight).data;
  const lastContentRow = findLastContentRow(pixels, sampleWidth, sampleHeight);
  const bottomPadding = Math.round(
    sampleWidth * COMIC_VIEWER_OPTIONS.bottomPaddingRatio,
  );
  const ratio = Math.min(1, (lastContentRow + bottomPadding) / sampleHeight);

  contentRatioCache.set(image.src, ratio);
  return ratio;
};
