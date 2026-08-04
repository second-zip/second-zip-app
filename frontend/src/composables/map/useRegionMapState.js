import { computed, ref } from 'vue';

import { SHORT_SIDO_NAMES } from '@/constants/map/regionMap';
import {
  getFeatureName,
  getGroupRegionCodes,
  getRegionCode,
  getRegionCodes,
  getSourceRegionCode,
} from '@/utils/map/regionMap';

export const useRegionMapState = () => {
  const currentLevel = ref('sido');
  const selectedSidoSourceCode = ref('');
  const selectedSidoRegionCode = ref('');
  const selectedSidoName = ref('');
  const selectedSourceRegionCode = ref('');
  const selectedFeature = ref(null);
  const hoveredRegionCode = ref('');
  const selectedCityName = ref('');
  const selectedCityKey = ref('');
  const selectedCityFeatures = ref([]);

  const selectedSidoDisplayName = computed(
    () => SHORT_SIDO_NAMES[selectedSidoName.value] || selectedSidoName.value,
  );
  const selectedRegionCodes = computed(() =>
    selectedFeature.value ? getRegionCodes(selectedFeature.value) : [],
  );
  const selectedRegionCode = computed(() =>
    selectedRegionCodes.value.length === 1 ? selectedRegionCodes.value[0] : '',
  );
  const selectedCityRegionCodes = computed(() =>
    getGroupRegionCodes({ features: selectedCityFeatures.value }),
  );

  const clearCity = () => {
    selectedCityName.value = '';
    selectedCityKey.value = '';
    selectedCityFeatures.value = [];
    selectedSourceRegionCode.value = '';
    selectedFeature.value = null;
    hoveredRegionCode.value = '';
  };

  const handleRegionSelect = (group, feature) => {
    const sourceCode = getSourceRegionCode(feature);
    if (currentLevel.value === 'sido') {
      selectedSidoSourceCode.value = sourceCode;
      selectedSidoRegionCode.value = getRegionCode(feature);
      selectedSidoName.value = String(getFeatureName(feature));
      selectedSourceRegionCode.value = '';
      selectedFeature.value = null;
      hoveredRegionCode.value = '';
      currentLevel.value = 'sigungu';
      return;
    }
    if (currentLevel.value === 'sigungu' && group.type === 'city') {
      selectedCityName.value = group.name;
      selectedCityKey.value = group.key;
      selectedCityFeatures.value = group.features;
      selectedSourceRegionCode.value = '';
      selectedFeature.value = null;
      hoveredRegionCode.value = '';
      currentLevel.value = 'district';
      return;
    }
    selectedSourceRegionCode.value = sourceCode;
    selectedFeature.value = feature;
  };

  const returnToSigungu = () => {
    clearCity();
    currentLevel.value = 'sigungu';
  };

  const resetToSido = () => {
    currentLevel.value = 'sido';
    selectedSidoSourceCode.value = '';
    selectedSidoRegionCode.value = '';
    selectedSidoName.value = '';
    clearCity();
  };

  return {
    currentLevel,
    selectedSidoSourceCode,
    selectedSidoRegionCode,
    selectedSidoName,
    selectedSidoDisplayName,
    selectedSourceRegionCode,
    selectedFeature,
    selectedRegionCode,
    selectedRegionCodes,
    selectedCityRegionCodes,
    hoveredRegionCode,
    selectedCityName,
    selectedCityKey,
    selectedCityFeatures,
    handleRegionSelect,
    returnToSigungu,
    resetToSido,
  };
};
