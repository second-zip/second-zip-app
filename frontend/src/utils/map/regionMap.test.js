import { describe, expect, it } from 'vitest';

import {
  createRegionGroups,
  getFeatureName,
  getGroupGeoJson,
  getGroupRegionCodes,
  getRegionCode,
  getRegionCodes,
  getSourceRegionCode,
  isValidRegionFeature,
  isValidRegionGeoJson,
  normalizeRegionFeature,
  parseCityDistrictName,
} from './regionMap';

const feature = ({
  name = '수원시 장안구',
  sourceRegionCode = '31111',
  regionCode = '41111',
  regionCodes,
  geometry = null,
} = {}) => ({
  type: 'Feature',
  properties: {
    name,
    sourceRegionCode,
    regionCode,
    ...(regionCodes === undefined ? {} : { regionCodes }),
  },
  geometry,
});

describe('regionMap util', () => {
  it('원본 코드, 백엔드 코드와 이름을 항상 문자열로 반환한다', () => {
    const target = feature({
      name: 123,
      sourceRegionCode: 31,
      regionCode: 41,
    });

    expect(getSourceRegionCode(target)).toBe('31');
    expect(getRegionCode(target)).toBe('41');
    expect(getFeatureName(target)).toBe('123');
    expect(getRegionCode(null)).toBe('');
  });

  it('regionCodes를 문자열로 정규화하고 중복과 빈 값을 제거한다', () => {
    const target = feature({ regionCodes: ['41590', 41591, '41590', '', null] });

    expect(getRegionCodes(target)).toEqual(['41590', '41591']);
    expect(getRegionCodes(feature({ regionCodes: undefined }))).toEqual([
      '41111',
    ]);
    expect(getRegionCodes(feature({ regionCode: null }))).toEqual([]);
  });

  it('OO시 OO구 형식만 부모 시와 일반구로 파싱한다', () => {
    expect(parseCityDistrictName(' 수원시 장안구 ')).toEqual({
      cityName: '수원시',
      districtName: '장안구',
    });
    expect(parseCityDistrictName('과천시')).toBeNull();
    expect(parseCityDistrictName('종로구')).toBeNull();
  });

  it('필수 properties와 백엔드 코드가 있는 feature만 유효하다', () => {
    expect(isValidRegionFeature(feature())).toBe(true);
    expect(isValidRegionFeature(feature({ sourceRegionCode: '' }))).toBe(false);
    expect(isValidRegionFeature(feature({ name: '' }))).toBe(false);
    expect(isValidRegionFeature(feature({ regionCode: null }))).toBe(false);
  });

  it('Polygon과 MultiPolygon의 ring 방향을 뒤집되 원본은 변경하지 않는다', () => {
    const ring = [[1, 1], [2, 2], [3, 3]];
    const polygon = feature({
      geometry: { type: 'Polygon', coordinates: [ring] },
    });
    const multiPolygon = feature({
      geometry: { type: 'MultiPolygon', coordinates: [[[...ring]]] },
    });

    expect(normalizeRegionFeature(polygon).geometry.coordinates[0]).toEqual([
      [3, 3],
      [2, 2],
      [1, 1],
    ]);
    expect(normalizeRegionFeature(multiPolygon).geometry.coordinates[0][0]).toEqual([
      [3, 3],
      [2, 2],
      [1, 1],
    ]);
    expect(ring[0]).toEqual([1, 1]);
    expect(normalizeRegionFeature(feature())).toEqual(feature());
  });

  it('FeatureCollection의 구조, feature 존재 여부와 properties를 검증한다', () => {
    expect(
      isValidRegionGeoJson({ type: 'FeatureCollection', features: [feature()] }),
    ).toBe(true);
    expect(isValidRegionGeoJson({ type: 'Feature', features: [feature()] })).toBe(
      false,
    );
    expect(isValidRegionGeoJson({ type: 'FeatureCollection', features: [] })).toBe(
      false,
    );
    expect(isValidRegionGeoJson({ type: 'FeatureCollection' })).toBe(false);
  });

  it('OO시 OO구 feature를 부모 시 그룹 하나로 묶고 일반 지역은 유지한다', () => {
    const jangAn = feature();
    const palDal = feature({
      name: '수원시 팔달구',
      sourceRegionCode: '31113',
      regionCode: '41113',
    });
    const gapyeong = feature({
      name: '가평군',
      sourceRegionCode: '31370',
      regionCode: '41820',
    });
    const groups = createRegionGroups([jangAn, palDal, gapyeong], '31');

    expect(groups).toHaveLength(2);
    expect(groups[0]).toMatchObject({
      type: 'city',
      key: 'city:31:수원시',
      name: '수원시',
      features: [jangAn, palDal],
    });
    expect(groups[1]).toMatchObject({
      type: 'region',
      key: 'region:31:31370',
      name: '가평군',
    });
  });

  it('그룹 GeoJSON과 중복 없는 백엔드 코드 목록을 만든다', () => {
    const hwaseong = feature({
      name: '화성시',
      regionCode: null,
      regionCodes: ['41591', '41592', '41593'],
    });
    const duplicate = feature({ regionCodes: ['41593'] });
    const group = { features: [hwaseong, duplicate] };

    expect(getGroupGeoJson(group)).toEqual({
      type: 'FeatureCollection',
      features: group.features,
    });
    expect(getGroupRegionCodes(group)).toEqual(['41591', '41592', '41593']);
  });
});
