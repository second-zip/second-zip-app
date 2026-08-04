import { computed } from 'vue';

import sidoGeoJson from '@/assets/maps/korea-sido.json';
import sigunguGeoJson from '@/assets/maps/korea-sigungu.json';
import { REGION_INSET_CONFIG } from '@/constants/map/regionMap';
import {
  createRegionGroups,
  getFeatureName,
  getSourceRegionCode,
  isValidRegionGeoJson,
  normalizeRegionFeature,
} from '@/utils/map/regionMap';

export const useRegionMapData = (state) => {
  const sidoIsValid = computed(() => isValidRegionGeoJson(sidoGeoJson));
  const sigunguIsValid = computed(() => isValidRegionGeoJson(sigunguGeoJson));
  const sidoFeatures = computed(() =>
    sidoIsValid.value ? sidoGeoJson.features.map(normalizeRegionFeature) : [],
  );
  const sigunguFeatures = computed(() =>
    sigunguIsValid.value
      ? sigunguGeoJson.features.map(normalizeRegionFeature)
      : [],
  );
  const filteredSigunguFeatures = computed(() =>
    sigunguFeatures.value.filter((feature) =>
      getSourceRegionCode(feature).startsWith(
        String(state.selectedSidoSourceCode.value),
      ),
    ),
  );
  const sigunguGroups = computed(() =>
    createRegionGroups(
      filteredSigunguFeatures.value,
      state.selectedSidoSourceCode.value,
    ),
  );
  const currentGeoJson = computed(() => {
    let features = filteredSigunguFeatures.value;
    if (state.currentLevel.value === 'sido') features = sidoFeatures.value;
    if (state.currentLevel.value === 'district') {
      features = state.selectedCityFeatures.value;
    }
    return features.length ? { type: 'FeatureCollection', features } : null;
  });
  const currentFeatures = computed(() => currentGeoJson.value?.features ?? []);
  const currentRenderGroups = computed(() => {
    if (state.currentLevel.value === 'sigungu') return sigunguGroups.value;
    return currentFeatures.value.map((feature) => ({
      type: 'region',
      key: `region:${state.currentLevel.value}:${getSourceRegionCode(feature)}`,
      name: getFeatureName(feature),
      features: [feature],
    }));
  });
  const projectionGeoJson = computed(() => {
    const geoJson = currentGeoJson.value;
    if (!geoJson?.features?.length || state.currentLevel.value !== 'sigungu') {
      return geoJson;
    }
    const features = geoJson.features.filter(
      (feature) => !REGION_INSET_CONFIG[getSourceRegionCode(feature)],
    );
    return { ...geoJson, features: features.length ? features : geoJson.features };
  });
  const hasInsetFeature = computed(
    () =>
      state.currentLevel.value === 'sigungu' &&
      currentFeatures.value.some(
        (feature) => REGION_INSET_CONFIG[getSourceRegionCode(feature)],
      ),
  );
  const errorMessage = computed(() => {
    if (state.currentLevel.value === 'sido' && !sidoIsValid.value) {
      return '지도 데이터를 불러올 수 없습니다.';
    }
    if (state.currentLevel.value !== 'sido' && !currentGeoJson.value) {
      return sigunguIsValid.value
        ? '해당 지역의 시군구 지도 데이터가 없습니다.'
        : '지도 데이터를 불러올 수 없습니다.';
    }
    return '';
  });

  return {
    currentRenderGroups,
    projectionGeoJson,
    hasInsetFeature,
    errorMessage,
  };
};
