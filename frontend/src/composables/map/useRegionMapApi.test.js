import { flushPromises } from '@vue/test-utils';
import { nextTick, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getFraudDamages, getJeonsePriceChanges } from '@/api/map';
import { JEONSE_PRICE_MONTH } from '@/constants/map/regionMap';
import { useRegionMapApi } from './useRegionMapApi';

vi.mock('@/api/map', () => ({
  getFraudDamages: vi.fn(),
  getJeonsePriceChanges: vi.fn(),
}));

const createState = (level = 'sido', sidoCode = '') => ({
  currentLevel: ref(level),
  selectedSidoRegionCode: ref(sidoCode),
});

describe('useRegionMapApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFraudDamages.mockResolvedValue({
      regions: [{ regionCode: '41', damageHouseCount: 10 }],
    });
    getJeonsePriceChanges.mockResolvedValue({
      regions: [{ regionCode: '41', changeRate: 0.12 }],
    });
  });

  it('최초 진입 시 두 API를 SIDO 조건으로 함께 조회한다', async () => {
    const dataType = ref('fraud-damage');
    const api = useRegionMapApi(createState(), dataType);
    await flushPromises();

    expect(getFraudDamages).toHaveBeenCalledWith({ level: 'SIDO' });
    expect(getJeonsePriceChanges).toHaveBeenCalledWith({
      level: 'SIDO',
      month: JEONSE_PRICE_MONTH,
    });
    expect(api.currentDataMap.value.get('41')).toMatchObject({
      damageHouseCount: 10,
    });
    expect(api.isCurrentMetricLoading.value).toBe(false);
    expect(api.currentMetricError.value).toBe('');
  });

  it('시도 진입 시 백엔드 시도 코드를 parentRegionCode로 조회한다', async () => {
    const state = createState();
    useRegionMapApi(state, ref('price-index'));
    await flushPromises();
    vi.clearAllMocks();

    state.selectedSidoRegionCode.value = '41';
    state.currentLevel.value = 'sigungu';
    await nextTick();
    await flushPromises();

    expect(getFraudDamages).toHaveBeenCalledWith({
      level: 'SIGUNGU',
      parentRegionCode: '41',
    });
    expect(getJeonsePriceChanges).toHaveBeenCalledWith({
      level: 'SIGUNGU',
      parentRegionCode: '41',
      month: JEONSE_PRICE_MONTH,
    });
  });

  it('district 단계에서는 재조회하지 않고 같은 시도의 캐시를 재사용한다', async () => {
    const state = createState('sigungu', '41');
    const api = useRegionMapApi(state, ref('fraud-damage'));
    await flushPromises();
    expect(getFraudDamages).toHaveBeenCalledTimes(1);
    vi.clearAllMocks();

    state.currentLevel.value = 'district';
    await nextTick();
    await flushPromises();

    expect(getFraudDamages).not.toHaveBeenCalled();
    expect(getJeonsePriceChanges).not.toHaveBeenCalled();
    expect(api.currentDataMap.value.get('41')).toMatchObject({
      damageHouseCount: 10,
    });
  });

  it('같은 레벨과 시도를 다시 방문하면 API를 재호출하지 않는다', async () => {
    const state = createState();
    useRegionMapApi(state, ref('fraud-damage'));
    await flushPromises();
    state.selectedSidoRegionCode.value = '41';
    state.currentLevel.value = 'sigungu';
    await nextTick();
    await flushPromises();
    expect(getFraudDamages).toHaveBeenCalledTimes(2);

    state.currentLevel.value = 'sido';
    await nextTick();
    state.currentLevel.value = 'sigungu';
    await nextTick();
    await flushPromises();

    expect(getFraudDamages).toHaveBeenCalledTimes(2);
    expect(getJeonsePriceChanges).toHaveBeenCalledTimes(2);
  });

  it('탭별 데이터와 오류 상태를 서로 독립적으로 관리한다', async () => {
    getFraudDamages.mockRejectedValueOnce(new Error('failure'));
    const dataType = ref('fraud-damage');
    const api = useRegionMapApi(createState(), dataType);
    await flushPromises();

    expect(api.currentMetricError.value).toBe(
      '지역 데이터를 불러오지 못했습니다.',
    );
    expect(api.currentDataMap.value.size).toBe(0);

    dataType.value = 'price-index';
    await nextTick();
    expect(api.currentMetricError.value).toBe('');
    expect(api.currentDataMap.value.get('41')).toMatchObject({
      changeRate: 0.12,
    });
  });

  it('regions 배열이 아닌 응답은 해당 metric의 오류로 처리한다', async () => {
    getJeonsePriceChanges.mockResolvedValueOnce({ data: [] });
    const api = useRegionMapApi(createState(), ref('price-index'));
    await flushPromises();

    expect(api.currentMetricError.value).toBe(
      '지역 데이터를 불러오지 못했습니다.',
    );
  });

  it('SIGUNGU인데 부모 백엔드 코드가 없으면 요청하지 않는다', async () => {
    useRegionMapApi(createState('sigungu', ''), ref('fraud-damage'));
    await flushPromises();

    expect(getFraudDamages).not.toHaveBeenCalled();
    expect(getJeonsePriceChanges).not.toHaveBeenCalled();
  });
});
