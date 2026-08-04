import { flushPromises } from '@vue/test-utils';
import { ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getFraudDamages, getJeonsePriceChanges } from '@/api/map';
import { useKoreaRegionMap } from './useKoreaRegionMap';

vi.mock('@/api/map', () => ({
  getFraudDamages: vi.fn(),
  getJeonsePriceChanges: vi.fn(),
}));

describe('useKoreaRegionMap', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFraudDamages.mockResolvedValue({
      regions: [{ regionCode: '11', damageHouseCount: 7 }],
    });
    getJeonsePriceChanges.mockResolvedValue({
      regions: [{ regionCode: '11', changeRate: -0.5 }],
    });
  });

  it('지도 그룹에 metric, 색상, 라벨, path 렌더링 정보를 결합한다', async () => {
    const map = useKoreaRegionMap(ref('fraud-damage'));
    await flushPromises();
    const seoul = map.currentRenderItems.value.find(
      (item) => item.name === '서울특별시',
    );

    expect(seoul).toMatchObject({
      displayName: '서울',
      metricValue: 7,
      displayValue: '7건',
      hasData: true,
      showLabel: true,
      fill: expect.any(String),
      labelPosition: expect.objectContaining({
        x: expect.any(Number),
        y: expect.any(Number),
      }),
    });
    expect(map.getPath(seoul.features[0])).toMatch(/^M/);
  });

  it('hover 그룹 key를 설정하고 초기화한다', () => {
    const map = useKoreaRegionMap(ref('fraud-damage'));

    map.setHoveredRegion({ key: 'region:sido:11' });
    expect(map.hoveredRegionCode.value).toBe('region:sido:11');
    map.clearHoveredRegion();
    expect(map.hoveredRegionCode.value).toBe('');
  });
});
