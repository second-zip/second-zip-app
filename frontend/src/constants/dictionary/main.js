// 도감 메인 화면의 고정 제목과 안내 문구입니다.
export const DICTIONARY_MAIN_COPY = Object.freeze({
  title: "전세사기 도감",
  description: "아래 메뉴에서 선택해 주세요",
});

// 카드별 표시 정보와 이동할 라우트를 정의합니다.
export const DICTIONARY_MAIN_CARDS = Object.freeze([
  {
    id: "word",
    title: "용어 정리",
    description: "헷갈리는 용어 한눈에",
    imageKey: "word",
    tone: "blue",
    routeName: "dictionary-words",
  },
  {
    id: "fraud",
    title: "전세사기 유형",
    description: "유형별 사기 양상 해설",
    imageKey: "fraud",
    tone: "purple",
    routeName: "dictionary-fraud",
  },
  {
    id: "register",
    title: "등기/건축 읽기",
    description: "등기부·건축물대장 해독",
    imageKey: "register",
    tone: "pink",
    routeName: "dictionary-register",
  },
  {
    id: "move-in",
    title: "전입/확정일자",
    description: "보증금 지키는 핵심 절차",
    imageKey: "move-in",
    tone: "green",
    routeName: "dictionary-move-in",
  },
]);
