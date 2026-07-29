// 분석 결과 화면의 기본 판정·비서·특약 표시 데이터를 관리하는 파일입니다.
import cautionIcon from '@/assets/icons/report/caution-yellow-22.svg';
import dangerIcon from '@/assets/icons/report/danger-red-22.svg';
import safeIcon from '@/assets/icons/report/safe-green-22.svg';
import catCaution from '@/assets/images/cat-caution.png';
import catDanger from '@/assets/images/cat-danger.png';
import catSafe from '@/assets/images/cat-safe.png';
import catDefault from '@/assets/images/cat.png';
import manCaution from '@/assets/images/main-caution.png';
import manDanger from '@/assets/images/main-danger.png';
import manSafe from '@/assets/images/man-safe.png';
import manDefault from '@/assets/images/man.png';
import womanCaution from '@/assets/images/woman-caution.png';
import womanDanger from '@/assets/images/woman-danger.png';
import womanSafe from '@/assets/images/woman-safe.png';
import womanDefault from '@/assets/images/woman.png';

export const DEFAULT_CHECKS = [
  // 필수 점검 1: 근저당 설정 금액
  {
    id: 'mortgage',
    label: '근저당',
    status: 'safe',
    basis: '등기부등본 기준 근저당권 설정 없음 확인',
    amount: '0원',
  },
  // 필수 점검 2: 위반건축물 표기 여부
  {
    id: 'violation',
    label: '위반건축물 표시',
    status: 'caution',
    basis: '건축물대장상 위반건축물 표기 여부 확인 필요',
    amount: '확인 필요',
  },
  // 필수 점검 3: 건축물 용도
  {
    id: 'residential',
    label: '주거용건축물',
    status: 'safe',
    basis: '건축물대장 기준 주거용 용도 확인',
    amount: '주거용',
  },
  // 필수 점검 4: HUG 보증보험 가입 가능 여부
  {
    id: 'hug',
    label: 'HUG가입가능여부',
    status: 'safe',
    basis: '입력 정보 기준 보증 가입 가능 범위',
    amount: '가입 가능',
  },
  // 필수 점검 5: 압류 등 권리침해 여부
  {
    id: 'rights',
    label: '권리침해여부',
    status: 'danger',
    basis: '등기부등본상 권리침해 항목 확인 필요',
    amount: '확인 필요',
  },
];

export const DEFAULT_FRAUD_TYPES = [
  // 유형 1: 무자본 갭투자·깡통전세형의 화면 기본 데이터
  {
    id: 'gap-investment',
    title: '유형 1. 무자본 갭투자·깡통전세형',
    subtitle: '전세가율·선순위채권·보증 가입 기준',
    items: [
      { label: 'A. 높은 전세가율', status: 'danger' },
      { label: 'B. 선순위채권 부담', status: 'caution' },
      { label: 'C. HUG보증보험 사전점검', status: 'safe' },
    ],
  },
  // 유형 2: 허위 정보·권리 은폐형의 화면 기본 데이터
  {
    id: 'false-information',
    title: '유형 2. 허위 정보·권리 은폐형',
    subtitle: '소유관계·용도·권리침해 기준',
    items: [
      { label: 'A. 건물·토지 소유관계 불일치', status: 'safe' },
      { label: 'B. 건축물 용도 허위 안내', status: 'caution' },
      { label: 'C. 등기상 권리침해 은폐', status: 'danger' },
    ],
  },
  // 유형 3: 신탁 부동산 사기형의 화면 기본 데이터
  {
    id: 'trust-property',
    title: '유형 3. 신탁 부동산 사기형',
    subtitle: '신탁등기·소유자·추가 권리침해 기준',
    items: [
      { label: 'A. 신탁등기 존재 여부', status: 'safe' },
      { label: 'B. 등기상 소유자 확인', status: 'safe' },
      { label: 'C. 신탁등기 이후 추가 권리침해 여부', status: 'caution' },
    ],
  },
];

// AI 연결 전 화면 확인용 특약 사항 더미 데이터
export const DEFAULT_SPECIAL_TERMS = [
  {
    title: '보증금 즉시 반환 명시',
    description:
      '계약 해지 또는 만료 시 임대인은 잔금 지급일로부터 3영업일 이내에 보증금 전액을 임차인에게 반환해야 한다.',
  },
  {
    title: '근저당 추가 설정 금지',
    description:
      '계약 기간 중 임대인은 해당 부동산에 신규 근저당권을 설정하거나 기존 채권최고액을 증액할 수 없다.',
  },
  {
    title: '잔금일 등기부등본 재확인',
    description:
      '임차인은 잔금 송금 직전 해당 부동산의 등기부등본을 재발급해 이상 유무를 확인한 후 지급하여야 한다.',
  },
  {
    title: '전세보증보험 가입 협조',
    description:
      '임대인은 임차인의 HUG·SGI 전세보증보험 가입을 위한 서류 제출 및 관련 절차에 적극 협조하여야 한다.',
  },
  {
    title: '매각·양도 시 임차인 우선 보호',
    description:
      '계약 기간 중 임대인이 해당 부동산을 제3자에게 매각 또는 양도할 경우 임차인에게 사전 고지하고 보증금 반환 의무를 승계하도록 조치해야 한다.',
  },
];

export const SPECIAL_TERMS_NOTICE =
  '* 본 특약은 AI 분석에 따른 권고사항이며,\n법적 효력은 계약서 작성 시 실제 내용에 따릅니다.';

export const RISK_ICONS = {
  safe: safeIcon,
  caution: cautionIcon,
  danger: dangerIcon,
};

export const RISK_LABELS = {
  safe: '안전',
  caution: '주의',
  danger: '위험',
};

// 안전도에 따른 비서 이미지 출력
export const SECRETARY_IMAGES = {
  cat: { safe: catSafe, caution: catCaution, danger: catDanger },
  man: { safe: manSafe, caution: manCaution, danger: manDanger },
  woman: { safe: womanSafe, caution: womanCaution, danger: womanDanger },
};

export const DEFAULT_SECRETARY_IMAGES = {
  cat: catDefault,
  man: manDefault,
  woman: womanDefault,
};

// 캐릭터와 위험도별 문구를 이곳에서 직접 수정하면 화면에 반영됩니다.
export const SECRETARY_MESSAGES = {
  // 고양이 분석 멘트
  cat: {
    safe: '안전이다냥!',
    caution: '조금 더 살펴보자냥!',
    danger: '위험하다냥!',
  },

  // 남자 위험 멘트
  man: {
    safe: '안전하네 나처럼',
    caution: '여기는 조심하는게 좋겠어',
    danger: '여기는 안돼!',
  },

  // 여자 위험 멘트
  woman: {
    safe: '안전한 집으로 추정됩니다.',
    caution: '확실히 주의가 필요합니다.',
    danger: '여긴 피하시길 권장합니다앗',
  },
};
