import { describe, expect, it } from 'vitest';

import {
  createRegionDataMap,
  getFeatureDamageHouseCount,
  getFeatureRegionRows,
  getGroupDamageHouseCount,
  getGroupRegionRows,
} from './regionData';

const feature = (regionCodes, mappingType = 'exact') => ({
  properties: { regionCodes, mappingType },
});

describe('regionData util', () => {
  const regions = [
    { regionCode: 1, damageHouseCount: 10 },
    { regionCode: '2', damageHouseCount: null },
    { regionCode: '3', damageHouseCount: '5' },
  ];

  it('배열 또는 regions 응답을 지역 코드 Map으로 변환한다', () => {
    expect(createRegionDataMap(regions).get('1')).toBe(regions[0]);
    expect(createRegionDataMap({ regions }).get('2')).toBe(regions[1]);
    expect(createRegionDataMap(null).size).toBe(0);
    expect(createRegionDataMap({ regions: null }).size).toBe(0);
  });

  it('feature의 모든 백엔드 코드에 연결된 row를 가져온다', () => {
    const dataMap = createRegionDataMap(regions);

    expect(getFeatureRegionRows(feature(['1', '3', 'missing']), dataMap)).toEqual([
      regions[0],
      regions[2],
    ]);
    expect(getFeatureRegionRows(feature(['1'], 'prefix'), dataMap)).toEqual([]);
  });

  it('feature 피해 건수를 합산하며 null은 0으로 처리한다', () => {
    const dataMap = createRegionDataMap(regions);

    expect(getFeatureDamageHouseCount(feature(['1', '2', '3']), dataMap)).toBe(
      15,
    );
  });

  it('그룹 row를 regionCode 기준으로 중복 제거하고 피해 건수를 합산한다', () => {
    const dataMap = createRegionDataMap(regions);
    const group = {
      features: [feature(['1', '2']), feature(['1', '3'])],
    };

    expect(getGroupRegionRows(group, dataMap)).toEqual(regions);
    expect(getGroupDamageHouseCount(group, dataMap)).toBe(15);
  });
});
