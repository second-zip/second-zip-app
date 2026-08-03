export const getFeatureCode = (feature) => feature?.properties?.regionCode;

export const getFeatureName = (feature) => feature?.properties?.regionName;

const CITY_DISTRICT_PATTERN = /^(.+?시)\s+(.+?구)$/;

export const parseCityDistrictName = (name) => {
  const match = String(name ?? '').trim().match(CITY_DISTRICT_PATTERN);
  if (!match) return null;

  return { cityName: match[1], districtName: match[2] };
};

export const isValidRegionFeature = (feature) => {
  const code = getFeatureCode(feature);
  const name = getFeatureName(feature);

  return code !== undefined && code !== null && name !== undefined && name !== null;
};

const reverseRing = (ring) => [...ring].reverse();

export const normalizeRegionFeature = (feature) => {
  const geometry = feature?.geometry;

  if (geometry?.type === 'Polygon') {
    return {
      ...feature,
      geometry: {
        ...geometry,
        coordinates: geometry.coordinates.map(reverseRing),
      },
    };
  }

  if (geometry?.type === 'MultiPolygon') {
    return {
      ...feature,
      geometry: {
        ...geometry,
        coordinates: geometry.coordinates.map((polygon) =>
          polygon.map(reverseRing),
        ),
      },
    };
  }

  return feature;
};

export const isValidRegionGeoJson = (geoJson) =>
  geoJson?.type === 'FeatureCollection' &&
  Array.isArray(geoJson.features) &&
  geoJson.features.length > 0 &&
  geoJson.features.every(isValidRegionFeature);

export const getGroupGeoJson = (group) => ({
  type: 'FeatureCollection',
  features: group.features,
});

export const createRegionGroups = (features, sidoCode) => {
  const groups = new Map();

  features.forEach((feature) => {
    const parsedName = parseCityDistrictName(getFeatureName(feature));
    const code = String(getFeatureCode(feature));
    const key = parsedName
      ? `city:${sidoCode}:${parsedName.cityName}`
      : `region:${sidoCode}:${code}`;

    if (!groups.has(key)) {
      groups.set(key, {
        type: parsedName ? 'city' : 'region',
        key,
        name: parsedName?.cityName || String(getFeatureName(feature)),
        features: [],
      });
    }
    groups.get(key).features.push(feature);
  });

  return [...groups.values()];
};
