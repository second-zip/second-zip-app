import { computed, ref } from 'vue';
import { describe, expect, it } from 'vitest';

import { REGION_INSET_CONFIG, SIDO_LABEL_OFFSET_CONFIG } from '@/constants/map/regionMap';
import { useRegionMapProjection } from './useRegionMapProjection';

const polygon = (name, sourceRegionCode, x = 126, y = 37) => ({
  type: 'Feature',
  properties: { name, sourceRegionCode },
  geometry: {
    type: 'Polygon',
    coordinates: [[
      [x, y],
      [x, y + 0.5],
      [x + 0.5, y + 0.5],
      [x + 0.5, y],
      [x, y],
    ]],
  },
});

const createContext = (features, level = 'sido') => {
  const currentLevel = ref(level);
  const projectionGeoJson = computed(() =>
    features.length ? { type: 'FeatureCollection', features } : null,
  );
  const state = {
    currentLevel,
    selectedSourceRegionCode: ref(''),
    hoveredRegionCode: ref(''),
  };
  const projection = useRegionMapProjection(state, { projectionGeoJson });
  return { state, projection };
};

describe('useRegionMapProjection', () => {
  it('현재 GeoJSON에 맞는 SVG path와 centroid를 계산한다', () => {
    const seoul = polygon('서울특별시', '11');
    const { projection } = createContext([seoul]);
    const group = { type: 'region', key: 'region:sido:11', name: '서울특별시', features: [seoul] };

    expect(projection.getPath(seoul)).toMatch(/^M/);
    expect(projection.getGroupLabelPosition(group)).toEqual(
      expect.objectContaining({ x: expect.any(Number), y: expect.any(Number) }),
    );
    expect(projection.getGroupDisplayName(group)).toBe('서울');
    expect(projection.shouldShowGroupLabel(group)).toBe(true);
  });

  it('시도 라벨에는 sourceRegionCode 기반 offset을 적용한다', () => {
    const seoul = polygon('서울특별시', '11');
    const { state, projection } = createContext([seoul]);
    const group = { type: 'region', key: 'region:sido:11', name: '서울특별시', features: [seoul] };
    const offsetPosition = projection.getGroupLabelPosition(group);
    state.currentLevel.value = 'district';
    const centerPosition = projection.getGroupLabelPosition(group);

    expect(offsetPosition.x - centerPosition.x).toBeCloseTo(
      SIDO_LABEL_OFFSET_CONFIG['11'].dx,
    );
    expect(offsetPosition.y - centerPosition.y).toBeCloseTo(
      SIDO_LABEL_OFFSET_CONFIG['11'].dy,
    );
  });

  it('district 단계에서는 부모 시 접두어를 제거한 구 이름을 표시한다', () => {
    const district = polygon('수원시 장안구', '31111');
    const { projection } = createContext([district], 'district');
    const group = { type: 'region', key: 'region:district:31111', name: '수원시 장안구', features: [district] };

    expect(projection.getGroupDisplayName(group)).toBe('장안구');
    expect(projection.getRegionAriaLabel(group, district)).toBe(
      '수원시 장안구 지도 선택',
    );
  });

  it('시군구의 부모 시 그룹에는 하위 구 지도 aria-label을 제공한다', () => {
    const district = polygon('수원시 장안구', '31111');
    const { projection } = createContext([district], 'sigungu');
    const group = { type: 'city', key: 'city:31:수원시', name: '수원시', features: [district] };

    expect(projection.getRegionAriaLabel(group, district)).toBe(
      '수원시 하위 구 지도 보기',
    );
  });

  it('시군구 라벨은 선택·hover 상태면 면적과 무관하게 표시한다', () => {
    const small = polygon('작은군', '31999');
    const { state, projection } = createContext([small], 'sigungu');
    const group = { type: 'region', key: 'region:31:31999', name: '작은군', features: [small] };

    state.selectedSourceRegionCode.value = '31999';
    expect(projection.shouldShowGroupLabel(group)).toBe(true);
    state.selectedSourceRegionCode.value = '';
    state.hoveredRegionCode.value = group.key;
    expect(projection.shouldShowGroupLabel(group)).toBe(true);
    expect(projection.isSelected(small)).toBe(false);
    state.selectedSourceRegionCode.value = '31999';
    expect(projection.isSelected(small)).toBe(true);
  });

  it('옹진군 인셋 path transform과 별도 라벨 위치를 계산한다', () => {
    const mainland = polygon('중구', '23010', 126, 37);
    const ongjin = polygon('옹진군', '23520', 125, 37.5);
    const { projection } = createContext([mainland, ongjin], 'sigungu');
    const group = { type: 'region', key: 'region:23:23520', name: '옹진군', features: [ongjin] };

    expect(REGION_INSET_CONFIG['23520']).toBeDefined();
    expect(projection.getFeatureTransform(ongjin)).toMatch(
      /^translate\(.+\) scale\(1\.2\) translate\(.+\)$/,
    );
    expect(projection.getFeatureTransform(mainland)).toBeUndefined();
    expect(projection.getGroupLabelPosition(group)).toEqual(
      expect.objectContaining({ x: expect.any(Number), y: expect.any(Number) }),
    );
    expect(projection.shouldShowGroupLabel(group)).toBe(true);
  });

  it('표시할 feature가 없으면 path와 centroid를 안전하게 반환한다', () => {
    const target = polygon('테스트', '00');
    const { projection } = createContext([]);
    const group = { type: 'region', key: 'region:sido:00', name: '테스트', features: [target] };

    expect(projection.getPath(target)).toBe('');
    expect(projection.getGroupLabelPosition(group)).toBeNull();
  });
});
