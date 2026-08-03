import catGuide from "@/assets/images/cat-main.png";
import buildingGuide from "@/assets/images/dictionary/guides/building-2x.png";
import registryGuide from "@/assets/images/dictionary/guides/registry-2x.png";
import moveInGuide from "@/assets/images/dictionary/guides/move-in-2x.png";
import fixedDateGuide from "@/assets/images/dictionary/guides/fixed-date-2x.png";

export const DICTIONARY_GUIDE_CONFIGS = Object.freeze({
  register: {
    pageTitle: "전세사기 도감",
    headerTitle: "등기부등본 / 건축물대장",
    guideImage: catGuide,
    guideMessage:
      "Ctrl + 마우스 휠로 필요한 부분을 확대하고\n드래그해서 자세히 볼 수 있다냥!",
    tabs: [
      {
        id: "registry",
        label: "등기부등본",
        images: [
          {
            id: "registry-guide",
            src: registryGuide,
            alt: "등기부등본 확인 방법을 설명하는 세로형 웹툰",
          },
        ],
      },
      {
        id: "building",
        label: "건축물대장",
        images: [
          {
            id: "building-guide",
            src: buildingGuide,
            alt: "건축물대장 확인 방법을 설명하는 세로형 웹툰",
          },
        ],
      },
    ],
  },
  moveIn: {
    pageTitle: "전세사기 도감",
    headerTitle: "전입신고 / 확정일자 부여",
    guideImage: catGuide,
    guideMessage:
      "Ctrl + 마우스 휠로 필요한 부분을 확대하고\n드래그해서 자세히 볼 수 있다냥!",
    tabs: [
      {
        id: "move-in-report",
        label: "전입신고",
        images: [
          {
            id: "move-in-guide",
            src: moveInGuide,
            alt: "전입신고 방법을 설명하는 세로형 웹툰",
          },
        ],
      },
      {
        id: "fixed-date",
        label: "확정일자 부여",
        images: [
          {
            id: "fixed-date-guide",
            src: fixedDateGuide,
            alt: "확정일자 부여 방법을 설명하는 세로형 웹툰",
          },
        ],
      },
    ],
  },
});

export const COMIC_VIEWER_OPTIONS = Object.freeze({
  minZoom: 1,
  maxZoom: 2.5,
  zoomStep: 0.1,
  compactGuideThreshold: 1.5,
  contentSampleWidth: 240,
  whiteThreshold: 244,
  minimumMarkedPixelRatio: 0.012,
  bottomPaddingRatio: 0.08,
});
