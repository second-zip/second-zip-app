export const MAP_WIDTH = 360;
export const MAP_HEIGHT = 300;
export const MAP_PADDING = 12;
export const SIGUNGU_LABEL_MIN_AREA = 220;
export const NO_DATA_COLOR = 'var(--black-100)';

export const RISK_COLOR_SCALE = [
  '#18aa68',
  '#c7f4df',
  '#fff0c9',
  '#f39a10',
  '#ffc4c6',
  '#f20d17',
];

export const DAMAGE_COLOR_SCALE = RISK_COLOR_SCALE;
export const JEONSE_COLOR_SCALE = RISK_COLOR_SCALE;

export const SIDO_LABEL_OFFSET_CONFIG = {
  11: { dx: 0, dy: -5 },
  23: { dx: -14, dy: -2 },
  26: { dx: 10, dy: 0 },
  31: { dx: 10, dy: 8 },
  33: { dx: -4, dy: -7 },
  34: { dx: -5, dy: -5 },
  36: { dx: 13, dy: 5 },
  37: { dx: 0, dy: -10 },
  29: { dx: 0, dy: -10 },
  25: { dx: 0, dy: 5 },
};

export const REGION_INSET_CONFIG = {
  23520: { distanceRatio: 0.16, labelDy: 0, scale: 1.2 },
  37630: { distanceRatio: 0.36, labelDy: 3, scale: 1.5 },
};

export const SHORT_SIDO_NAMES = {
  서울특별시: '서울',
  부산광역시: '부산',
  대구광역시: '대구',
  인천광역시: '인천',
  광주광역시: '광주',
  대전광역시: '대전',
  울산광역시: '울산',
  세종특별자치시: '세종',
  경기도: '경기',
  강원특별자치도: '강원',
  충청북도: '충북',
  충청남도: '충남',
  전북특별자치도: '전북',
  전라남도: '전남',
  경상북도: '경북',
  경상남도: '경남',
  제주특별자치도: '제주',
};
