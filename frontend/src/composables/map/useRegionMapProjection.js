import { computed } from 'vue';
import { geoMercator, geoPath } from 'd3-geo';

import {
  MAP_HEIGHT,
  MAP_PADDING,
  MAP_WIDTH,
  REGION_INSET_CONFIG,
  SHORT_SIDO_NAMES,
  SIDO_LABEL_OFFSET_CONFIG,
  SIGUNGU_LABEL_MIN_AREA,
} from '@/constants/map/regionMap';
import {
  getFeatureCode,
  getFeatureName,
  getGroupGeoJson,
  parseCityDistrictName,
} from '@/utils/map/regionMap';

export const useRegionMapProjection = (state, data) => {
  const projection = computed(() => {
    const geoJson = data.projectionGeoJson.value;
    if (!geoJson?.features?.length) return null;
    return geoMercator().fitExtent(
      [
        [MAP_PADDING, MAP_PADDING],
        [MAP_WIDTH - MAP_PADDING, MAP_HEIGHT - MAP_PADDING],
      ],
      geoJson,
    );
  });
  const pathGenerator = computed(() =>
    projection.value ? geoPath(projection.value) : null,
  );
  const getPath = (feature) => pathGenerator.value?.(feature) || '';
  const getInsetConfig = (feature) =>
    state.currentLevel.value === 'sigungu'
      ? REGION_INSET_CONFIG[String(getFeatureCode(feature))] || null
      : null;
  const getInsetPosition = (feature, inset) => {
    if (!pathGenerator.value || !data.projectionGeoJson.value) return null;
    const [featureX, featureY] = pathGenerator.value.centroid(feature);
    const [anchorX, anchorY] = pathGenerator.value.centroid(
      data.projectionGeoJson.value,
    );
    if (![featureX, featureY, anchorX, anchorY].every(Number.isFinite)) {
      return null;
    }
    return {
      featureX,
      featureY,
      targetX: anchorX + (featureX - anchorX) * inset.distanceRatio,
      targetY: anchorY + (featureY - anchorY) * inset.distanceRatio,
    };
  };
  const getFeatureTransform = (feature) => {
    const inset = getInsetConfig(feature);
    if (!inset) return undefined;
    const position = getInsetPosition(feature, inset);
    if (!position) return undefined;
    return `translate(${position.targetX} ${position.targetY}) scale(${inset.scale}) translate(${-position.featureX} ${-position.featureY})`;
  };
  const getGroupCentroid = (group) => {
    if (!pathGenerator.value) return null;
    const [x, y] = pathGenerator.value.centroid(getGroupGeoJson(group));
    return Number.isFinite(x) && Number.isFinite(y) ? { x, y } : null;
  };
  const getGroupLabelPosition = (group) => {
    const feature = group.features[0];
    const inset = group.type === 'region' ? getInsetConfig(feature) : null;
    if (inset) {
      const position = getInsetPosition(feature, inset);
      if (position) {
        return { x: position.targetX, y: position.targetY + inset.labelDy };
      }
    }
    const center = getGroupCentroid(group);
    if (!center || state.currentLevel.value !== 'sido') return center;
    const offset = SIDO_LABEL_OFFSET_CONFIG[String(getFeatureCode(feature))];
    return offset
      ? { x: center.x + offset.dx, y: center.y + offset.dy }
      : center;
  };
  const getGroupDisplayName = (group) => {
    if (state.currentLevel.value === 'sido') {
      return SHORT_SIDO_NAMES[group.name] || group.name;
    }
    if (state.currentLevel.value === 'district') {
      return parseCityDistrictName(group.name)?.districtName || group.name;
    }
    return group.name;
  };
  const shouldShowGroupLabel = (group) => {
    if (state.currentLevel.value !== 'sigungu') return true;
    const feature = group.features[0];
    const selected = group.features.some(
      (item) => String(getFeatureCode(item)) === state.selectedSigunguCode.value,
    );
    if (
      getInsetConfig(feature) ||
      selected ||
      state.hoveredRegionCode.value === group.key
    ) {
      return true;
    }
    return (
      (pathGenerator.value?.area(getGroupGeoJson(group)) ?? 0) >=
      SIGUNGU_LABEL_MIN_AREA
    );
  };
  const isSelected = (feature) =>
    state.currentLevel.value !== 'sido' &&
    String(getFeatureCode(feature)) === state.selectedSigunguCode.value;
  const getRegionAriaLabel = (group, feature) =>
    state.currentLevel.value === 'sigungu' && group.type === 'city'
      ? `${group.name} 하위 구 지도 보기`
      : `${getFeatureName(feature)} 지도 선택`;

  return {
    getPath,
    getFeatureTransform,
    getGroupLabelPosition,
    getGroupDisplayName,
    shouldShowGroupLabel,
    isSelected,
    getRegionAriaLabel,
  };
};
