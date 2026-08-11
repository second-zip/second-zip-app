import { describe, expect, it } from 'vitest';

import {
  DAMAGE_COLOR_SCALE,
  JEONSE_COLOR_SCALE,
  NO_DATA_COLOR,
} from '@/constants/map/regionMap';
import {
  getGroupMetric,
  getMetricColorIndex,
  getRegionFill,
} from './regionMetric';

const feature = (regionCodes) => ({
  properties: { regionCodes, mappingType: 'exact' },
});
const group = (regionCodes) => ({ features: [feature(regionCodes)] });
const dataMap = (rows) =>
  new Map(rows.map((row) => [String(row.regionCode), row]));

describe('regionMetric util', () => {
  it('연결 row가 없으면 데이터 없음 metric을 반환한다', () => {
    expect(getGroupMetric(group(['1']), new Map(), 'fraud-damage')).toEqual({
      metricValue: null,
      displayValue: '-',
      hasData: false,
    });
  });

  it('피해 건수는 다중 지역 값을 합산하고 0도 데이터로 유지한다', () => {
    const rows = [
      { regionCode: '1', damageHouseCount: 0 },
      { regionCode: '2', damageHouseCount: 20 },
      { regionCode: '3', damageHouseCount: 30 },
    ];

    expect(
      getGroupMetric(group(['1', '2', '3']), dataMap(rows), 'fraud-damage'),
    ).toEqual({ metricValue: 50, displayValue: '50건', hasData: true });
    expect(
      getGroupMetric(group(['1']), dataMap(rows), 'fraud-damage'),
    ).toEqual({ metricValue: 0, displayValue: '0건', hasData: true });
  });

  it('전세가격 변동률은 유효한 다중 지역 값의 평균을 표시한다', () => {
    const rows = [
      { regionCode: '1', changeRate: '0.1234' },
      { regionCode: '2', changeRate: -0.1 },
      { regionCode: '3', changeRate: null },
    ];

    const metric = getGroupMetric(
      group(['1', '2', '3']),
      dataMap(rows),
      'price-index',
    );

    expect(metric.metricValue).toBeCloseTo(0.0117);
    expect(metric).toMatchObject({ displayValue: '0.01%', hasData: true });
  });

  it('유효한 변동률이 없으면 데이터 없음으로 처리한다', () => {
    const rows = [
      { regionCode: '1', changeRate: '' },
      { regionCode: '2', changeRate: 'not-a-number' },
    ];

    expect(
      getGroupMetric(group(['1', '2']), dataMap(rows), 'price-index'),
    ).toEqual({ metricValue: null, displayValue: '-', hasData: false });
  });

  it('현재 값의 6분위 위치를 색상 index로 계산한다', () => {
    const values = [1, 2, 3, 4, 5, 6];

    expect(getMetricColorIndex(1, values)).toBe(0);
    expect(getMetricColorIndex(4, values)).toBe(3);
    expect(getMetricColorIndex(6, values)).toBe(5);
    expect(getMetricColorIndex(1, [])).toBe(2);
    expect(getMetricColorIndex(7, [7, 7])).toBe(2);
  });

  it('데이터 없음은 중립색, 피해 건수는 원본값 기준 색상을 사용한다', () => {
    expect(
      getRegionFill({ hasData: false, metricValue: null }, [], 'fraud-damage'),
    ).toBe(NO_DATA_COLOR);
    expect(
      getRegionFill({ hasData: true, metricValue: 6 }, [1, 2, 3, 4, 5, 6], 'fraud-damage'),
    ).toBe(DAMAGE_COLOR_SCALE[5]);
  });

  it('전세가격 변동 위험도는 부호가 아닌 절대값 크기로 분류한다', () => {
    const metricValues = [-6, 1, 2, 3, 4, 5];

    expect(
      getRegionFill({ hasData: true, metricValue: -6 }, metricValues, 'price-index'),
    ).toBe(JEONSE_COLOR_SCALE[5]);
    expect(
      getRegionFill({ hasData: true, metricValue: 1 }, metricValues, 'price-index'),
    ).toBe(JEONSE_COLOR_SCALE[0]);
  });
});
