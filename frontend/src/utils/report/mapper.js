// 백엔드 분석 리포트 응답을 공통 결과 화면 데이터로 변환하는 파일입니다.
import { formatKoreanDeposit } from './analysis.js';
import { normalizeCharacterType } from '../character.js';

const RISK_LEVELS = {
  SAFE: 'safe',
  CAUTION: 'caution',
  DANGER: 'danger',
};
const CHECK_META = {
  MORTGAGE_EXISTENCE: { id: 'mortgage', label: '근저당' },
  ILLEGAL_BUILDING: { id: 'violation', label: '위반건축물 표시' },
  BUILDING_USE: { id: 'residential', label: '주거용건축물' },
  HUG_GUARANTEE_ELIGIBILITY: { id: 'hug', label: 'HUG가입가능여부' },
  RIGHTS_INFRINGEMENT: { id: 'rights', label: '권리침해여부' },
};

const FRAUD_META = {
  UNDERWATER_JEONSE: {
    id: 'gap-investment',
    title: '유형 1. 무자본 갭투자·깡통전세형',
    subtitle: '전세가율·선순위채권·보증 가입 기준',
  },
  FALSE_INFORMATION_RIGHTS_CONCEALMENT: {
    id: 'false-information',
    title: '유형 2. 허위 정보·권리 은폐형',
    subtitle: '소유관계·용도·권리침해 기준',
  },
  TRUST_PROPERTY_FRAUD: {
    id: 'trust-property',
    title: '유형 3. 신탁 부동산 사기형',
    subtitle: '신탁등기·소유자·추가 권리침해 기준',
  },
};

const DETAIL_LABELS = {
  HIGH_JEONSE_RATIO: 'A. 높은 전세가율',
  PRIORITY_DEBT_BURDEN: 'B. 선순위채권 부담',
  HUG_GUARANTEE_PRECHECK: 'C. HUG보증보험 사전점검',
  LAND_BUILDING_OWNERSHIP_MISMATCH: 'A. 건물·토지 소유관계 불일치',
  FALSE_BUILDING_USE_INFORMATION: 'B. 건축물 용도 허위 안내',
  RIGHTS_INFRINGEMENT_CONCEALMENT: 'C. 등기상 권리침해 은폐',
  TRUST_REGISTRATION_EXISTENCE: 'A. 신탁등기 존재 여부',
  REGISTERED_OWNER_VERIFICATION: 'B. 등기상 소유자 확인',
  POST_TRUST_RIGHTS_INFRINGEMENT: 'C. 신탁등기 이후 추가 권리침해 여부',
};

const hasValue = (value) => value !== null && value !== undefined;
const formatWon = (value) =>
  hasValue(value) ? formatKoreanDeposit(Number(value) || 0) : '-';

const makeCheckCopy = (checkType, evidence = {}, status) => {
  switch (checkType) {
    case 'MORTGAGE_EXISTENCE':
      return {
        basis: '등기부등본 기준 근저당권 설정 금액',
        amount: formatWon(evidence.mortgageAmount),
      };
    case 'ILLEGAL_BUILDING':
      return {
        basis: '건축물대장상 위반건축물 표기 여부',
        amount: !hasValue(evidence.isIllegalBuilding)
          ? '-'
          : evidence.isIllegalBuilding
            ? '위반건축물'
            : '해당 없음',
      };
    case 'BUILDING_USE':
      return {
        basis: '건축물대장 기준 건축물 용도',
        amount: evidence.buildingUse ?? '-',
      };
    case 'HUG_GUARANTEE_ELIGIBILITY':
      return {
        basis: `보증금 ${formatWon(evidence.deposit)} · 기준가 ${formatWon(evidence.basePrice)}`,
        amount:
          status === 'safe'
            ? '가입 가능'
            : status === 'caution'
              ? '확인 필요'
              : '가입 어려움',
      };
    case 'RIGHTS_INFRINGEMENT':
      return {
        basis: '등기부등본상 압류 등 권리침해 여부',
        amount: !hasValue(evidence.hasSeizure)
          ? '-'
          : evidence.hasSeizure
            ? '권리침해 있음'
            : '해당 없음',
      };
    default:
      return { basis: '판정 근거 확인 필요', amount: '-' };
  }
};

// 백엔드 위험도 enum을 화면에서 사용하는 상태값으로 변환합니다.
export const toUiRisk = (riskLevel) =>
  RISK_LEVELS[String(riskLevel).toUpperCase()] ?? 'caution';

// 필수 점검 응답을 기존 아코디언 디자인의 데이터 구조로 변환합니다.
export const mapCheckResults = (results = []) =>
  results.map((result) => {
    const meta = CHECK_META[result.checkType] ?? {
      id: result.checkType,
      label: result.checkType,
    };
    const status = toUiRisk(result.result);

    return {
      ...meta,
      status,
      ...makeCheckCopy(result.checkType, result.evidence, status),
    };
  });

// 전세사기 유형과 세부 판정을 기존 유형 카드의 데이터 구조로 변환합니다.
export const mapFraudTypes = (types = []) =>
  types.map((type) => {
    const meta = FRAUD_META[type.fraudType] ?? {
      id: type.fraudType,
      title: type.fraudType,
      subtitle: '세부 판정 결과',
    };

    return {
      ...meta,
      status: toUiRisk(type.riskLevel),
      items: (type.detailResults ?? []).map((detail) => ({
        label: DETAIL_LABELS[detail.detailType] ?? detail.detailType,
        status: toUiRisk(detail.result),
      })),
    };
  });

// 향후 AI API의 특약 응답을 화면의 제목·설명 구조로 정규화합니다.
export const mapSpecialTerms = (terms) =>
  Array.isArray(terms)
    ? terms.map((term, index) => ({
        title: term?.title ?? `AI 추천 특약 ${index + 1}`,
        content: term?.content ?? '-',
      }))
    : [];

// 미리보기 또는 사용자 설정의 비서 타입을 화면에서 사용하는 값으로 변환합니다.
export const mapSecretary = (secretary) =>
  normalizeCharacterType(secretary, null);

// 상세 리포트 응답에서 화면이 바로 사용할 데이터만 추출합니다.
export const mapReportDetail = (report = {}) => ({
  analysisReportId: report.analysisReportId,
  address:
    [report.roadAddress, report.detailAddress].filter(Boolean).join(' ') || '-',
  deposit: hasValue(report.deposit) ? String(report.deposit) : '-',
  risk: toUiRisk(report.result),
  favorite: Boolean(report.favorite),
  secretary: mapSecretary(report.secretary ?? report.characterType),
  checks: mapCheckResults(report.checkResults),
  fraudTypes: mapFraudTypes(report.fraudTypes),
  // TODO: AI/ChatGPT API 연결 시 aiSpecialTerms에 받은 특약 배열을 전달합니다.
  specialTerms: mapSpecialTerms(report.aiSpecialTerms ?? report.specialTerms),
});
