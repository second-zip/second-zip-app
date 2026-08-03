import { computed, ref } from 'vue';

import { SHORT_SIDO_NAMES } from '@/constants/map/regionMap';
import { getFeatureCode, getFeatureName } from '@/utils/map/regionMap';

export const useRegionMapState = () => {
  const currentLevel = ref('sido');
  const selectedSidoCode = ref('');
  const selectedSidoName = ref('');
  const selectedSigunguCode = ref('');
  const hoveredRegionCode = ref('');
  const selectedCityName = ref('');
  const selectedCityKey = ref('');
  const selectedCityFeatures = ref([]);

  const selectedSidoDisplayName = computed(
    () => SHORT_SIDO_NAMES[selectedSidoName.value] || selectedSidoName.value,
  );

  const clearCity = () => {
    selectedCityName.value = '';
    selectedCityKey.value = '';
    selectedCityFeatures.value = [];
    selectedSigunguCode.value = '';
    hoveredRegionCode.value = '';
  };

  const handleRegionSelect = (group, feature) => {
    const code = String(getFeatureCode(feature));
    if (currentLevel.value === 'sido') {
      selectedSidoCode.value = code;
      selectedSidoName.value = String(getFeatureName(feature));
      selectedSigunguCode.value = '';
      hoveredRegionCode.value = '';
      currentLevel.value = 'sigungu';
      return;
    }
    if (currentLevel.value === 'sigungu' && group.type === 'city') {
      selectedCityName.value = group.name;
      selectedCityKey.value = group.key;
      selectedCityFeatures.value = group.features;
      selectedSigunguCode.value = '';
      hoveredRegionCode.value = '';
      currentLevel.value = 'district';
      return;
    }
    selectedSigunguCode.value = code;
  };

  const returnToSigungu = () => {
    clearCity();
    currentLevel.value = 'sigungu';
  };

  const resetToSido = () => {
    currentLevel.value = 'sido';
    selectedSidoCode.value = '';
    selectedSidoName.value = '';
    clearCity();
  };

  return {
    currentLevel,
    selectedSidoCode,
    selectedSidoName,
    selectedSidoDisplayName,
    selectedSigunguCode,
    hoveredRegionCode,
    selectedCityName,
    selectedCityKey,
    selectedCityFeatures,
    handleRegionSelect,
    returnToSigungu,
    resetToSido,
  };
};
