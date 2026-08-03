import catWord from "@/assets/images/cat-dict-1.png";
import catFraud from "@/assets/images/cat-dict-2.png";
import catRegister from "@/assets/images/cat-dict-3.png";
import catMoveIn from "@/assets/images/cat-dict-4.png";
import catGuide from "@/assets/images/cat-main.png";

export const DICTIONARY_MAIN_COPY = Object.freeze({
  title: "전세사기 도감",
  description: "아래 메뉴에서 선택해 주세요",
  guide: "전세사기는 알아야 막는다냥!\n아는 게 힘, 같이 성장해 보자냥?",
});

export const DICTIONARY_MAIN_CARDS = Object.freeze([
  {
    id: "word",
    title: "용어 정리",
    description: "헷갈리는 용어 한눈에",
    image: catWord,
    tone: "blue",
    routeName: "dictionary-words",
  },
  {
    id: "fraud",
    title: "전세사기 유형",
    description: "유형별 사기 양상 해설",
    image: catFraud,
    tone: "purple",
    routeName: "dictionary-fraud",
  },
  {
    id: "register",
    title: "등기/건축 읽기",
    description: "등기부·건축물대장 해독",
    image: catRegister,
    tone: "pink",
    routeName: "dictionary-register",
  },
  {
    id: "move-in",
    title: "전입/확정일자",
    description: "보증금 지키는 핵심 절차",
    image: catMoveIn,
    tone: "green",
    routeName: "dictionary-move-in",
  },
]);

export const DICTIONARY_GUIDE_IMAGE = catGuide;
