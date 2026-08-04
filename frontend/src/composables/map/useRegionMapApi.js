import { computed, ref, watch } from 'vue';

import { getFraudDamages, getJeonsePriceChanges } from '@/api/map';
import { JEONSE_PRICE_MONTH } from '@/constants/map/regionMap';
import { createRegionDataMap } from '@/utils/map/regionData';

export const useRegionMapApi = (state, dataType) => {
  const fraudCache = ref(new Map());
  const priceCache = ref(new Map());
  const fraudPending = ref(new Set());
  const pricePending = ref(new Set());
  const fraudErrors = ref(new Map());
  const priceErrors = ref(new Map());

  const queryKey = computed(() =>
    state.currentLevel.value === 'sido'
      ? 'SIDO'
      : `SIGUNGU:${state.selectedSidoRegionCode.value}`,
  );
  const getParams = () => {
    if (state.currentLevel.value === 'sido') return { level: 'SIDO' };
    const parentRegionCode = state.selectedSidoRegionCode.value;
    return parentRegionCode
      ? { level: 'SIGUNGU', parentRegionCode }
      : null;
  };
  const setCollectionValue = (collection, key, value) => {
    const next = new Map(collection.value);
    next.set(key, value);
    collection.value = next;
  };
  const setPending = (pending, key, value) => {
    const next = new Set(pending.value);
    value ? next.add(key) : next.delete(key);
    pending.value = next;
  };
  const loadMetric = async ({ cache, errors, pending, key, request }) => {
    if (cache.value.has(key) || pending.value.has(key)) return;
    setPending(pending, key, true);
    setCollectionValue(errors, key, '');
    try {
      const response = await request();
      if (!Array.isArray(response?.regions)) {
        throw new Error('지역 데이터 응답 형식이 올바르지 않습니다.');
      }
      setCollectionValue(cache, key, response.regions);
    } catch {
      setCollectionValue(errors, key, '지역 데이터를 불러오지 못했습니다.');
    } finally {
      setPending(pending, key, false);
    }
  };
  const loadCurrentLevel = () => {
    const params = getParams();
    if (!params) return;
    const key = queryKey.value;
    if (dataType.value === 'fraud-damage') {
      loadMetric({
        cache: fraudCache,
        errors: fraudErrors,
        pending: fraudPending,
        key,
        request: () => getFraudDamages(params),
      });
      return;
    }
    loadMetric({
      cache: priceCache,
      errors: priceErrors,
      pending: pricePending,
      key,
      request: () =>
        getJeonsePriceChanges({ ...params, month: JEONSE_PRICE_MONTH }),
    });
  };

  watch(
    [state.currentLevel, state.selectedSidoRegionCode, dataType],
    loadCurrentLevel,
    { immediate: true },
  );

  const fraudDamageRegions = computed(
    () => fraudCache.value.get(queryKey.value) ?? [],
  );
  const jeonsePriceRegions = computed(
    () => priceCache.value.get(queryKey.value) ?? [],
  );
  const fraudDamageMap = computed(() =>
    createRegionDataMap(fraudDamageRegions.value),
  );
  const jeonsePriceMap = computed(() =>
    createRegionDataMap(jeonsePriceRegions.value),
  );
  const currentDataMap = computed(() =>
    dataType.value === 'fraud-damage'
      ? fraudDamageMap.value
      : jeonsePriceMap.value,
  );
  const isCurrentMetricLoading = computed(() =>
    (dataType.value === 'fraud-damage' ? fraudPending : pricePending).value.has(
      queryKey.value,
    ),
  );
  const currentMetricError = computed(
    () =>
      (dataType.value === 'fraud-damage'
        ? fraudErrors
        : priceErrors
      ).value.get(queryKey.value) ?? '',
  );

  return {
    currentDataMap,
    isCurrentMetricLoading,
    currentMetricError,
  };
};
