// 공통 분석 결과 화면을 A~F 시나리오와 장문 특약으로 검증하는 미리보기 데이터 파일입니다.
const SCENARIO_F_LONG_TERM_TEXT = (
  '임대인은 계약 체결일부터 임대차 종료와 보증금 반환 완료일까지 임차인의 권리를 침해할 수 있는 근저당권, 전세권, 압류, 가압류, 가처분, 신탁등기 등의 권리변동을 새로 발생시키지 않으며, 관련 변동 가능성이 생기면 즉시 임차인에게 서면으로 알리고 필요한 보호조치에 협조해야 합니다. '
).repeat(60).slice(0, 6000);

// 백엔드 연결 없이 분석 상세 화면을 확인하는 ReportDetailResponse 형태의 mock 데이터입니다.
export const MOCK_REPORT_DETAIL = {
  analysisReportId: 999,
  secretary: 'woman',
  roadAddress: '서울특별시 강남구 테헤란로 1',
  detailAddress: '101동 101호',
  deposit: 100000000,
  result: 'SAFE',
  favorite: true,
  checkResults: [
    // 필수 점검 1: 근저당 설정 금액
    {
      checkType: 'MORTGAGE_EXISTENCE',
      result: 'SAFE',
      evidence: { mortgageAmount: 0 },
    },
    // 필수 점검 2: 위반건축물 표기 여부
    {
      checkType: 'ILLEGAL_BUILDING',
      result: 'SAFE',
      evidence: { isIllegalBuilding: false },
    },
    // 필수 점검 3: 건축물 용도
    {
      checkType: 'BUILDING_USE',
      result: 'SAFE',
      evidence: { buildingUse: '공동주택' },
    },
    // 필수 점검 4: HUG 보증보험 가입 가능 여부
    {
      checkType: 'HUG_GUARANTEE_ELIGIBILITY',
      result: 'SAFE',
      evidence: {
        deposit: 100_000_000,
        mortgageAmount: 0,
        basePrice: 180_722_892,
      },
    },
    // 필수 점검 5: 압류 등 권리침해 여부
    {
      checkType: 'RIGHTS_INFRINGEMENT',
      result: 'SAFE',
      evidence: { hasSeizure: false },
    },
  ],
  fraudTypes: [
    // 유형 1: 무자본 갭투자·깡통전세형의 A/B/C 판정 데이터
    {
      fraudType: 'UNDERWATER_JEONSE',
      riskLevel: 'SAFE',
      detailResults: [
        { detailType: 'HIGH_JEONSE_RATIO', result: 'SAFE' },
        { detailType: 'PRIORITY_DEBT_BURDEN', result: 'SAFE' },
        { detailType: 'HUG_GUARANTEE_PRECHECK', result: 'SAFE' },
      ],
    },
    // 유형 2: 허위 정보·권리 은폐형의 A/B/C 판정 데이터
    {
      fraudType: 'FALSE_INFORMATION_RIGHTS_CONCEALMENT',
      riskLevel: 'SAFE',
      detailResults: [
        {
          detailType: 'LAND_BUILDING_OWNERSHIP_MISMATCH',
          result: 'SAFE',
        },
        {
          detailType: 'FALSE_BUILDING_USE_INFORMATION',
          result: 'SAFE',
        },
        {
          detailType: 'RIGHTS_INFRINGEMENT_CONCEALMENT',
          result: 'SAFE',
        },
      ],
    },
    // 유형 3: 신탁 부동산 사기형의 A/B/C 판정 데이터
    {
      fraudType: 'TRUST_PROPERTY_FRAUD',
      riskLevel: 'SAFE',
      detailResults: [
        { detailType: 'TRUST_REGISTRATION_EXISTENCE', result: 'SAFE' },
        { detailType: 'REGISTERED_OWNER_VERIFICATION', result: 'SAFE' },
        {
          detailType: 'POST_TRUST_RIGHTS_INFRINGEMENT',
          result: 'SAFE',
        },
      ],
    },
  ],
};

// 시나리오 B: 전세가율 85%로 깡통전세 위험 판정을 확인하는 화면용 응답입니다.
export const SCENARIO_B_REPORT_DETAIL = {
  ...MOCK_REPORT_DETAIL,
  analysisReportId: 998,
  secretary: 'man',
  roadAddress: '서울특별시 마포구 월드컵로 22',
  detailAddress: '101호',
  deposit: 170_000_000,
  result: 'DANGER',
  checkResults: MOCK_REPORT_DETAIL.checkResults.map((check) =>
    check.checkType === 'HUG_GUARANTEE_ELIGIBILITY'
      ? {
          ...check,
          result: 'CAUTION',
          evidence: {
            deposit: 170_000_000,
            mortgageAmount: 0,
            basePrice: 200_000_000,
          },
        }
      : check,
  ),
  fraudTypes: MOCK_REPORT_DETAIL.fraudTypes.map((fraudType) =>
    fraudType.fraudType === 'UNDERWATER_JEONSE'
      ? {
          ...fraudType,
          riskLevel: 'DANGER',
          detailResults: fraudType.detailResults.map((detail) =>
            detail.detailType === 'HIGH_JEONSE_RATIO'
              ? { ...detail, result: 'DANGER' }
              : detail,
          ),
        }
      : fraudType,
  ),
};

// 시나리오 C: 신탁등기·신탁회사 소유·추가 권리침해가 모두 위험인 응답입니다.
export const SCENARIO_C_REPORT_DETAIL = {
  ...MOCK_REPORT_DETAIL,
  analysisReportId: 997,
  secretary: 'cat',
  roadAddress: '경기도 수원시 팔달구 정조로 33',
  detailAddress: '101호',
  deposit: 100_000_000,
  result: 'DANGER',
  checkResults: MOCK_REPORT_DETAIL.checkResults.map((check) =>
    check.checkType === 'HUG_GUARANTEE_ELIGIBILITY'
      ? {
          ...check,
          evidence: {
            deposit: 100_000_000,
            mortgageAmount: 0,
            basePrice: 250_000_000,
          },
        }
      : check,
  ),
  fraudTypes: MOCK_REPORT_DETAIL.fraudTypes.map((fraudType) =>
    fraudType.fraudType === 'TRUST_PROPERTY_FRAUD'
      ? {
          ...fraudType,
          riskLevel: 'DANGER',
          detailResults: fraudType.detailResults.map((detail) => ({
            ...detail,
            result: 'DANGER',
          })),
        }
      : fraudType,
  ),
};

// 시나리오 D: 근저당·위반건축물·다가구 HUG 위험을 함께 확인하는 응답입니다.
export const SCENARIO_D_REPORT_DETAIL = {
  ...MOCK_REPORT_DETAIL,
  analysisReportId: 996,
  secretary: 'woman',
  roadAddress: '인천광역시 부평구 부평대로 44',
  detailAddress: '101호',
  deposit: 80_000_000,
  result: 'DANGER',
  checkResults: MOCK_REPORT_DETAIL.checkResults.map((check) => {
    if (check.checkType === 'MORTGAGE_EXISTENCE') {
      return {
        ...check,
        result: 'CAUTION',
        evidence: { mortgageAmount: 120_000_000 },
      };
    }
    if (check.checkType === 'ILLEGAL_BUILDING') {
      return {
        ...check,
        result: 'DANGER',
        evidence: { isIllegalBuilding: true },
      };
    }
    if (check.checkType === 'HUG_GUARANTEE_ELIGIBILITY') {
      return {
        ...check,
        result: 'CAUTION',
        evidence: {
          deposit: 80_000_000,
          mortgageAmount: 120_000_000,
          basePrice: 250_000_000,
        },
      };
    }
    return check;
  }),
  fraudTypes: MOCK_REPORT_DETAIL.fraudTypes.map((fraudType) =>
    fraudType.fraudType === 'UNDERWATER_JEONSE'
      ? {
          ...fraudType,
          riskLevel: 'CAUTION',
          detailResults: fraudType.detailResults.map((detail) =>
            detail.detailType === 'PRIORITY_DEBT_BURDEN'
              ? { ...detail, result: 'CAUTION' }
              : detail,
          ),
        }
      : fraudType,
  ),
};

// 시나리오 E: 가격 확인 불가·압류·소유 불일치·비주거 용도의 복합 위험 응답입니다.
export const SCENARIO_E_REPORT_DETAIL = {
  ...MOCK_REPORT_DETAIL,
  analysisReportId: 995,
  roadAddress: '부산광역시 해운대구 센텀로 55',
  detailAddress: '101호',
  deposit: 100_000_000,
  result: 'DANGER',
  checkResults: MOCK_REPORT_DETAIL.checkResults.map((check) => {
    if (check.checkType === 'BUILDING_USE') {
      return {
        ...check,
        result: 'DANGER',
        evidence: { buildingUse: '근린생활시설' },
      };
    }
    if (check.checkType === 'HUG_GUARANTEE_ELIGIBILITY') {
      return {
        ...check,
        result: 'CAUTION',
        evidence: {
          deposit: 100_000_000,
          mortgageAmount: 0,
          basePrice: 0,
        },
      };
    }
    if (check.checkType === 'RIGHTS_INFRINGEMENT') {
      return {
        ...check,
        result: 'DANGER',
        evidence: { hasSeizure: true },
      };
    }
    return check;
  }),
  fraudTypes: MOCK_REPORT_DETAIL.fraudTypes.map((fraudType) => {
    if (fraudType.fraudType === 'UNDERWATER_JEONSE') {
      return {
        ...fraudType,
        riskLevel: 'DANGER',
        detailResults: fraudType.detailResults.map((detail) => ({
          ...detail,
          result: 'CAUTION',
        })),
      };
    }
    if (fraudType.fraudType === 'FALSE_INFORMATION_RIGHTS_CONCEALMENT') {
      return {
        ...fraudType,
        riskLevel: 'DANGER',
        detailResults: fraudType.detailResults.map((detail) => ({
          ...detail,
          result: 'DANGER',
        })),
      };
    }
    return fraudType;
  }),
};

// 시나리오 F: 보증금 1억과 근저당·건축물 용도·HUG 주의 3개를 확인하는 응답입니다.
export const SCENARIO_F_REPORT_DETAIL = {
  ...MOCK_REPORT_DETAIL,
  analysisReportId: 994,
  roadAddress: '서울특별시 서초구 검증로 66',
  detailAddress: '101호',
  deposit: 100_000_000,
  result: 'DANGER',
  // TODO: AI/ChatGPT API 연결 후 aiSpecialTerms를 실제 추천 특약 응답으로 교체합니다.
  aiSpecialTerms: [
    {
      title: '장문 특약 반응형 높이 테스트',
      content: SCENARIO_F_LONG_TERM_TEXT,
    },
  ],
  checkResults: MOCK_REPORT_DETAIL.checkResults.map((check) => {
    if (check.checkType === 'MORTGAGE_EXISTENCE') {
      return {
        ...check,
        result: 'CAUTION',
        evidence: { mortgageAmount: 50_000_000 },
      };
    }
    if (check.checkType === 'BUILDING_USE') {
      return {
        ...check,
        result: 'CAUTION',
        evidence: { buildingUse: '업무용 오피스텔' },
      };
    }
    if (check.checkType === 'HUG_GUARANTEE_ELIGIBILITY') {
      return {
        ...check,
        result: 'CAUTION',
        evidence: {
          deposit: 100_000_000,
          mortgageAmount: 50_000_000,
          basePrice: 200_000_000,
        },
      };
    }
    return check;
  }),
  fraudTypes: MOCK_REPORT_DETAIL.fraudTypes.map((fraudType) =>
    fraudType.fraudType === 'UNDERWATER_JEONSE'
      ? {
          ...fraudType,
          riskLevel: 'CAUTION',
          detailResults: fraudType.detailResults.map((detail) =>
            detail.detailType === 'PRIORITY_DEBT_BURDEN'
              ? { ...detail, result: 'CAUTION' }
              : detail,
          ),
        }
      : fraudType.fraudType === 'FALSE_INFORMATION_RIGHTS_CONCEALMENT'
        ? {
            ...fraudType,
            riskLevel: 'CAUTION',
            detailResults: fraudType.detailResults.map((detail) =>
              detail.detailType === 'FALSE_BUILDING_USE_INFORMATION'
                ? { ...detail, result: 'CAUTION' }
                : detail,
            ),
          }
        : fraudType,
  ),
};

// 미리보기 경로의 시나리오 키와 공통 화면 응답을 연결합니다.
export const ANALYSIS_PREVIEW_REPORTS = Object.freeze({
  a: MOCK_REPORT_DETAIL,
  b: SCENARIO_B_REPORT_DETAIL,
  c: SCENARIO_C_REPORT_DETAIL,
  d: SCENARIO_D_REPORT_DETAIL,
  e: SCENARIO_E_REPORT_DETAIL,
  f: SCENARIO_F_REPORT_DETAIL,
});
