import { beforeEach, describe, expect, it, vi } from 'vitest';

import api from './instance';
import { getFraudDamages, getJeonsePriceChanges } from './map';

vi.mock('./instance', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('map API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('피해 현황 조회 파라미터를 전달하고 data를 반환한다', async () => {
    const data = { regions: [{ regionCode: '41', damageHouseCount: 10 }] };
    api.get.mockResolvedValue({ data });

    await expect(
      getFraudDamages({ level: 'SIGUNGU', parentRegionCode: '41' }),
    ).resolves.toBe(data);
    expect(api.get).toHaveBeenCalledWith('/maps/fraud-damage', {
      params: { level: 'SIGUNGU', parentRegionCode: '41' },
    });
  });

  it('전세가격 변동 조회 파라미터를 전달하고 data를 반환한다', async () => {
    const data = { regions: [{ regionCode: '11', changeRate: -0.12 }] };
    api.get.mockResolvedValue({ data });

    await expect(
      getJeonsePriceChanges({ level: 'SIDO', month: '202606' }),
    ).resolves.toBe(data);
    expect(api.get).toHaveBeenCalledWith('/maps/jeonse-price', {
      params: { level: 'SIDO', month: '202606' },
    });
  });

  it('파라미터를 생략하면 빈 객체를 사용한다', async () => {
    api.get.mockResolvedValue({ data: { regions: [] } });

    await getFraudDamages();
    await getJeonsePriceChanges();

    expect(api.get).toHaveBeenNthCalledWith(1, '/maps/fraud-damage', {
      params: {},
    });
    expect(api.get).toHaveBeenNthCalledWith(2, '/maps/jeonse-price', {
      params: {},
    });
  });
});
