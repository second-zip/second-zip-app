import { describe, expect, it } from 'vitest';

import { REGION_INSET_CONFIG } from '@/constants/map/regionMap';
import { getSourceRegionCode } from '@/utils/map/regionMap';
import { useRegionMapData } from './useRegionMapData';
import { useRegionMapState } from './useRegionMapState';

const selectSido = (state, group) => {
  state.handleRegionSelect(group, group.features[0]);
};

describe('useRegionMapData', () => {
  it('최초 화면에 유효한 17개 시도 그룹을 제공한다', () => {
    const state = useRegionMapState();
    const data = useRegionMapData(state);

    expect(data.currentRenderGroups.value).toHaveLength(17);
    expect(data.projectionGeoJson.value.type).toBe('FeatureCollection');
    expect(data.errorMessage.value).toBe('');
    expect(data.hasInsetFeature.value).toBe(false);
  });

  it('경기도를 선택하면 sourceRegionCode로 필터링하고 부모 시를 그룹화한다', () => {
    const state = useRegionMapState();
    const data = useRegionMapData(state);
    const gyeonggi = data.currentRenderGroups.value.find(
      (group) => group.name === '경기도',
    );

    selectSido(state, gyeonggi);

    expect(data.currentRenderGroups.value.length).toBeGreaterThan(1);
    expect(
      data.projectionGeoJson.value.features.every((feature) =>
        getSourceRegionCode(feature).startsWith('31'),
      ),
    ).toBe(true);
    const suwon = data.currentRenderGroups.value.find(
      (group) => group.name === '수원시',
    );
    expect(suwon.type).toBe('city');
    expect(suwon.features.length).toBeGreaterThan(1);
    expect(suwon.features.every((item) => item.properties.name.startsWith('수원시 '))).toBe(
      true,
    );
  });

  it('부모 시를 클릭하면 해당 일반구만 district 그룹으로 표시한다', () => {
    const state = useRegionMapState();
    const data = useRegionMapData(state);
    selectSido(
      state,
      data.currentRenderGroups.value.find((group) => group.name === '경기도'),
    );
    const suwon = data.currentRenderGroups.value.find(
      (group) => group.name === '수원시',
    );

    state.handleRegionSelect(suwon, suwon.features[0]);

    expect(state.currentLevel.value).toBe('district');
    expect(data.currentRenderGroups.value).toHaveLength(suwon.features.length);
    expect(data.currentRenderGroups.value.every((group) => group.type === 'region')).toBe(
      true,
    );
  });

  it('인천 시군구 projection에서 옹진군을 제외하지만 렌더 그룹에는 유지한다', () => {
    const state = useRegionMapState();
    const data = useRegionMapData(state);
    selectSido(
      state,
      data.currentRenderGroups.value.find((group) => group.name === '인천광역시'),
    );
    const renderedCodes = data.currentRenderGroups.value.flatMap((group) =>
      group.features.map(getSourceRegionCode),
    );
    const projectionCodes = data.projectionGeoJson.value.features.map(
      getSourceRegionCode,
    );

    expect(renderedCodes).toContain('23520');
    expect(REGION_INSET_CONFIG['23520']).toBeDefined();
    expect(projectionCodes).not.toContain('23520');
    expect(data.hasInsetFeature.value).toBe(true);
  });

  it('연결되는 시군구가 없는 시도 코드에는 오류 메시지를 제공한다', () => {
    const state = useRegionMapState();
    const data = useRegionMapData(state);
    state.selectedSidoSourceCode.value = '00';
    state.currentLevel.value = 'sigungu';

    expect(data.currentRenderGroups.value).toEqual([]);
    expect(data.projectionGeoJson.value).toBeNull();
    expect(data.errorMessage.value).toBe(
      '해당 지역의 시군구 지도 데이터가 없습니다.',
    );
  });
});
