import { getRegionCodes } from '@/utils/map/regionMap';

export const createRegionDataMap = (apiResponse) =>
  new Map(
    (Array.isArray(apiResponse)
      ? apiResponse
      : Array.isArray(apiResponse?.regions)
        ? apiResponse.regions
        : []
    ).map(
      (region) => [String(region.regionCode), region],
    ),
  );

export const getFeatureRegionRows = (feature, regionDataMap) =>
  feature?.properties?.mappingType === 'prefix'
    ? []
    : getRegionCodes(feature)
        .map((code) => regionDataMap.get(code))
        .filter(Boolean);

export const getFeatureDamageHouseCount = (feature, regionDataMap) =>
  getFeatureRegionRows(feature, regionDataMap).reduce(
    (sum, region) => sum + Number(region.damageHouseCount ?? 0),
    0,
  );

export const getGroupRegionRows = (group, regionDataMap) =>
  [
    ...new Map(
      group.features
        .flatMap((feature) => getFeatureRegionRows(feature, regionDataMap))
        .map((region) => [String(region.regionCode), region]),
    ).values(),
  ];

export const getGroupDamageHouseCount = (group, regionDataMap) =>
  getGroupRegionRows(group, regionDataMap).reduce(
    (sum, region) => sum + Number(region.damageHouseCount ?? 0),
    0,
  );
