export const TOTAL_ANALYSIS_STEPS = 6;

export const ANALYSIS_STEP_MESSAGES = [
  '분석 환경을 확인하고 있어요.',
  '집 정보를 확인하고 있어요.',
  '건물을 분석하고 있어요.',
  '동/호수를 분석하고 있어요.',
  '집의 위험도를 분석하고 있어요.',
  '계약에 필요한 특약을 만들고 있어요.',
];

export const ANALYSIS_STEP_FAILURE_MESSAGES = {
  1: '분석 환경을 확인하지 못했습니다.',
  2: '분석 요청을 시작하지 못했습니다.',
  3: '인증을 시작하지 못했습니다.',
  4: '인증을 완료하지 못했습니다.',
  5: '분석을 완료하지 못했습니다.',
  6: '계약 특약을 만들지 못했습니다.',
};

export const AUTH_POLL_INTERVAL_MS = 2_000;
export const MAX_AUTH_TRANSITIONS = 30;
export const DEMO_PROGRESS_INTERVAL_MS = 500;
export const DEMO_FALLBACK_REPORT_ID = 16;

export const AUTH_SELECTION_ACTIONS = new Set([
  'ADDRESS_SELECTION',
  'DONG_SELECTION',
  'HO_SELECTION',
]);
