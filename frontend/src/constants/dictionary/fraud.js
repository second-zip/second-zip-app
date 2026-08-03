import catGuide from "@/assets/images/cat-main.png";

export const FRAUD_DICTIONARY_COPY = Object.freeze({
  pageTitle: "전세사기 도감",
  sectionTitle: "전세사기 유형",
  guide: "전세사기 유형이다냥.\n글 읽기 싫으면 쇼폼 영상으로 보라냥~",
});

export const FRAUD_DICTIONARY_GUIDE_IMAGE = catGuide;

export const FRAUD_TYPES = Object.freeze([
  {
    id: "gap-investment",
    number: 1,
    title: "무자본 갭투자·깡통전세형",
    hashtags: [
      "깡통전세",
      "전세가율확인",
      "보증금반환위험",
      "선순위채권확인",
    ],
    description:
      "집값보다 전세금이 높거나 임대인이 거의 자본 없이 전세금을 활용해 주택을 매입하는 유형입니다. 집값이 떨어지거나 경매로 넘어가면 임차인이 보증금을 모두 돌려받지 못할 위험이 큽니다.",
    videoSrc: "",
  },
  {
    id: "rights-concealment",
    number: 2,
    title: "허위 정보·권리 은폐형",
    hashtags: [
      "허위정보",
      "소유자성명일치",
      "등기부권리확인",
      "건축물용도확인",
    ],
    description:
      "소유관계, 용도, 대출, 압류, 선순위 권리 등 계약 판단에 중요한 정보를 허위로 알리거나 숨기는 유형입니다. 등기부등본과 건축물대장을 직접 확인하고 계약 직전에도 권리 변동을 다시 점검해야 합니다.",
    videoSrc: "",
  },
  {
    id: "trust-property",
    number: 3,
    title: "신탁 부동산 사기형",
    hashtags: [
      "신탁사기",
      "성명일치확인",
      "신탁원부확인",
      "신탁회사동의여부",
    ],
    description:
      "신탁등기된 부동산에서 신탁회사의 동의 없이 권한 없는 사람과 계약하게 만드는 유형입니다. 등기부등본의 신탁등기와 신탁원부, 정당한 임대 권한과 동의 여부를 반드시 확인해야 합니다.",
    videoSrc: "",
  },
]);
