import { useRegionMapData } from '@/composables/map/useRegionMapData';
import { useRegionMapProjection } from '@/composables/map/useRegionMapProjection';
import { useRegionMapState } from '@/composables/map/useRegionMapState';

export const useKoreaRegionMap = () => {
  const state = useRegionMapState();
  const data = useRegionMapData(state);
  const projection = useRegionMapProjection(state, data);

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
    setHoveredRegion,
    clearHoveredRegion,
  };
};
