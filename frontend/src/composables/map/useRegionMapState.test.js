import { describe, expect, it } from 'vitest';

import { useRegionMapState } from './useRegionMapState';

const feature = ({
  name,
  sourceRegionCode,
  regionCode,
  regionCodes = [regionCode],
}) => ({
  properties: { name, sourceRegionCode, regionCode, regionCodes },
});

const gyeonggi = feature({
  name: '경기도',
  sourceRegionCode: '31',
  regionCode: '41',
});
const jangAn = feature({
  name: '수원시 장안구',
  sourceRegionCode: '31111',
  regionCode: '41111',
});
const palDal = feature({
  name: '수원시 팔달구',
  sourceRegionCode: '31113',
  regionCode: '41113',
});

describe('useRegionMapState', () => {
  it('시도를 선택하면 시군구 단계와 백엔드 부모 코드를 설정한다', () => {
    const state = useRegionMapState();

    state.handleRegionSelect({ type: 'region' }, gyeonggi);

    expect(state.currentLevel.value).toBe('sigungu');
    expect(state.selectedSidoSourceCode.value).toBe('31');
    expect(state.selectedSidoRegionCode.value).toBe('41');
    expect(state.selectedSidoName.value).toBe('경기도');
    expect(state.selectedSidoDisplayName.value).toBe('경기');
  });

  it('부모 시 그룹을 선택하면 district 단계와 모든 하위 코드를 저장한다', () => {
    const state = useRegionMapState();
    const city = {
      type: 'city',
      key: 'city:31:수원시',
      name: '수원시',
      features: [jangAn, palDal],
    };
    state.currentLevel.value = 'sigungu';

    state.handleRegionSelect(city, jangAn);

    expect(state.currentLevel.value).toBe('district');
    expect(state.selectedCityName.value).toBe('수원시');
    expect(state.selectedCityKey.value).toBe('city:31:수원시');
    expect(state.selectedCityFeatures.value).toEqual([jangAn, palDal]);
    expect(state.selectedCityRegionCodes.value).toEqual(['41111', '41113']);
  });

  it('일반 지역과 district의 구 선택은 feature 및 단일·다중 코드를 제공한다', () => {
    const state = useRegionMapState();
    state.currentLevel.value = 'sigungu';

    state.handleRegionSelect({ type: 'region' }, jangAn);

    expect(state.selectedSourceRegionCode.value).toBe('31111');
    expect(state.selectedFeature.value).toEqual(jangAn);
    expect(state.selectedRegionCodes.value).toEqual(['41111']);
    expect(state.selectedRegionCode.value).toBe('41111');

    const hwaseong = feature({
      name: '화성시',
      sourceRegionCode: '31240',
      regionCode: null,
      regionCodes: ['41591', '41592', '41593'],
    });
    state.handleRegionSelect({ type: 'region' }, hwaseong);

    expect(state.selectedRegionCodes.value).toEqual(['41591', '41592', '41593']);
    expect(state.selectedRegionCode.value).toBe('');
  });

  it('district에서 시군구로 돌아가면 부모 시 선택만 초기화한다', () => {
    const state = useRegionMapState();
    state.currentLevel.value = 'district';
    state.selectedSidoRegionCode.value = '41';
    state.selectedCityName.value = '수원시';
    state.selectedCityFeatures.value = [jangAn];
    state.selectedFeature.value = jangAn;

    state.returnToSigungu();

    expect(state.currentLevel.value).toBe('sigungu');
    expect(state.selectedSidoRegionCode.value).toBe('41');
    expect(state.selectedCityName.value).toBe('');
    expect(state.selectedCityFeatures.value).toEqual([]);
    expect(state.selectedFeature.value).toBeNull();
  });

  it('전국으로 돌아가면 모든 탐색 및 선택 상태를 초기화한다', () => {
    const state = useRegionMapState();
    state.handleRegionSelect({ type: 'region' }, gyeonggi);
    state.selectedSourceRegionCode.value = '31111';
    state.selectedFeature.value = jangAn;
    state.hoveredRegionCode.value = 'region:31:31111';

    state.resetToSido();

    expect(state.currentLevel.value).toBe('sido');
    expect(state.selectedSidoSourceCode.value).toBe('');
    expect(state.selectedSidoRegionCode.value).toBe('');
    expect(state.selectedSidoName.value).toBe('');
    expect(state.selectedSourceRegionCode.value).toBe('');
    expect(state.selectedFeature.value).toBeNull();
    expect(state.hoveredRegionCode.value).toBe('');
  });
});
