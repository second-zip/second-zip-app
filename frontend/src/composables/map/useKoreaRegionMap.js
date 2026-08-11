import { useRegionMapData } from '@/composables/map/useRegionMapData';
import { useRegionMapProjection } from '@/composables/map/useRegionMapProjection';
import { useRegionMapState } from '@/composables/map/useRegionMapState';
import { getRegionFill, getGroupMetric } from '@/utils/map/regionMetric';

export const useKoreaRegionMap = (dataType) => {
  const state = useRegionMapState();
  const data = useRegionMapData(state);
  const projection = useRegionMapProjection(state, data);
  const api = useRegionMapApi(state, dataType);

  const metricItems = computed(() =>
    data.currentRenderGroups.value.map((group) => ({
      ...group,
      ...getGroupMetric(group, api.currentDataMap.value, dataType.value),
    })),
  );
  const currentMetricValues = computed(() =>
    metricItems.value
      .filter((item) => item.hasData)
      .map((item) => item.metricValue),
  );
  const currentRenderItems = computed(() =>
    metricItems.value.map((item) => ({
      ...item,
      fill: getRegionFill(item, currentMetricValues.value, dataType.value),
      displayName: projection.getGroupDisplayName(item),
      labelPosition: projection.getGroupLabelPosition(item),
      showLabel: projection.shouldShowGroupLabel(item),
    })),
  );

  const setHoveredRegion = (group) => {
    state.hoveredRegionCode.value = group.key;
  };
  const clearHoveredRegion = () => {
    state.hoveredRegionCode.value = '';
  };
  return {
    ...state,
    ...data,
    ...projection,
    ...api,
    currentRenderItems,
    setHoveredRegion,
    clearHoveredRegion,
  };
};
import { computed } from 'vue';

import { useRegionMapApi } from '@/composables/map/useRegionMapApi';
