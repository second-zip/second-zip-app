import { readFile, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { GROUP_REGION_OVERRIDES } from './regionCodeOverrides.mjs';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const frontendDir = resolve(scriptDir, '../..');
const initSqlPath = resolve(frontendDir, '../docker/init.sql');
const sidoPath = resolve(frontendDir, 'src/assets/maps/korea-sido.json');
const sigunguPath = resolve(frontendDir, 'src/assets/maps/korea-sigungu.json');
const CITY_DISTRICT_PATTERN = /^(.+?시)\s+(.+?구)$/;

const parseBackendRegions = (sql) => {
  const sidoSection = sql.slice(
    sql.indexOf('-- 시도 17개'),
    sql.indexOf('-- HUG 통계 대상'),
  );
  const sido = [...sidoSection.matchAll(
    /\('([^']+)',\s*'([^']+)',\s*'SIDO',\s*NULL\)/g,
  )].map((match) => ({
    regionCode: match[1],
    regionName: match[2],
    regionLevel: 'SIDO',
    parentRegionCode: null,
  }));
  const sigungu = [...sql.matchAll(
    /INSERT INTO regions \(region_code, region_name, region_level, parent_region_id\) SELECT '([^']+)', '([^']+)', '([^']+)', region_id FROM regions WHERE region_code = '([^']+)'/g,
  )].map((match) => ({
    regionCode: match[1],
    regionName: match[2],
    regionLevel: match[3],
    parentRegionCode: match[4],
  }));
  if (sido.length !== 17 || !sigungu.length) {
    throw new Error('init.sql의 지역 INSERT 구조를 파싱할 수 없습니다.');
  }
  return { sido, sigungu };
};

const getSourceCode = (properties) => String(
  properties.sourceRegionCode ??
    properties.code ??
    properties.regionCode ??
    '',
);
const getSourceName = (properties) => String(
  properties.name ?? properties.regionName ?? '',
).trim();
const getComparableNames = (name) => {
  const districtMatch = name.match(CITY_DISTRICT_PATTERN);
  return new Set([name, districtMatch?.[2]].filter(Boolean));
};
const serializeGeoJson = (geoJson) =>
  `{"type":"FeatureCollection", "features": [\n${geoJson.features
    .map((feature) => JSON.stringify(feature))
    .join(',\n')}\n]}\n`;
const geometrySnapshot = (geoJson) =>
  JSON.stringify(geoJson.features.map((feature) => feature.geometry));

const createProperties = (properties, mapping) => {
  const { regionName, code, ...preserved } = properties;
  return { ...preserved, ...mapping };
};

const main = async () => {
  const [sql, sidoText, sigunguText] = await Promise.all([
    readFile(initSqlPath, 'utf8'),
    readFile(sidoPath, 'utf8'),
    readFile(sigunguPath, 'utf8'),
  ]);
  const backend = parseBackendRegions(sql);
  const sidoGeoJson = JSON.parse(sidoText);
  const sigunguGeoJson = JSON.parse(sigunguText);
  const geometryBefore = {
    sido: geometrySnapshot(sidoGeoJson),
    sigungu: geometrySnapshot(sigunguGeoJson),
  };
  const backendSidoByName = new Map(
    backend.sido.map((region) => [region.regionName, region]),
  );
  const sidoMapping = new Map();

  for (const feature of sidoGeoJson.features) {
    const sourceRegionCode = getSourceCode(feature.properties);
    const name = getSourceName(feature.properties);
    if (!sourceRegionCode) throw new Error(`시도 ${name}: sourceRegionCode 누락`);
    const matched = backendSidoByName.get(name);
    if (!matched) throw new Error(`시도 ${name}: 백엔드 exact 매핑 실패`);
    sidoMapping.set(sourceRegionCode, matched);
    feature.properties = createProperties(feature.properties, {
      name,
      sourceRegionCode,
      regionCode: matched.regionCode,
      regionCodes: [matched.regionCode],
      mappingType: 'exact',
    });
  }

  const backendBySido = new Map();
  for (const region of backend.sigungu) {
    const regions = backendBySido.get(region.parentRegionCode) ?? [];
    regions.push(region);
    backendBySido.set(region.parentRegionCode, regions);
  }
  const usedBackendCodes = new Set();
  const prefixFallbacks = [];
  const summary = { sidoExact: sidoGeoJson.features.length, exact: 0, prefix: 0, group: 0 };

  for (const feature of sigunguGeoJson.features) {
    const sourceRegionCode = getSourceCode(feature.properties);
    const name = getSourceName(feature.properties);
    if (!sourceRegionCode) throw new Error(`${name}: sourceRegionCode 누락`);
    if (sourceRegionCode.length <= 2) throw new Error(`${name}: 원본 코드 길이 오류`);
    const sourceSidoCode = sourceRegionCode.slice(0, 2);
    const backendSido = sidoMapping.get(sourceSidoCode);
    if (!backendSido) throw new Error(`${name}: 부모 시도 매핑 실패`);
    const groupOverride = GROUP_REGION_OVERRIDES[sourceRegionCode];

    if (groupOverride) {
      const candidates = backend.sigungu.filter(
        (region) =>
          region.parentRegionCode === groupOverride.backendSidoCode &&
          groupOverride.backendRegionNames.includes(region.regionName),
      );
      const foundCodes = new Set(candidates.map((region) => region.regionCode));
      if (
        groupOverride.regionCodes.some((code) => !foundCodes.has(code)) ||
        new Set(groupOverride.regionCodes).size !== groupOverride.regionCodes.length
      ) {
        throw new Error(`${name}: 화성시 group 코드 검증 실패`);
      }
      groupOverride.regionCodes.forEach((code) => usedBackendCodes.add(code));
      feature.properties = createProperties(feature.properties, {
        name,
        sourceRegionCode,
        regionCode: null,
        regionCodes: groupOverride.regionCodes,
        mappingType: 'group',
      });
      summary.group += 1;
      continue;
    }

    const comparableNames = getComparableNames(name);
    const candidates = (backendBySido.get(backendSido.regionCode) ?? []).filter(
      (region) => comparableNames.has(region.regionName),
    );
    if (candidates.length > 1) {
      throw new Error(`${name}: 같은 시도·이름의 백엔드 후보가 중복됩니다.`);
    }
    let regionCode;
    let mappingType;
    if (candidates.length === 1) {
      regionCode = candidates[0].regionCode;
      mappingType = 'exact';
      usedBackendCodes.add(regionCode);
      summary.exact += 1;
    } else {
      regionCode = `${backendSido.regionCode}${sourceRegionCode.slice(2)}`;
      mappingType = 'prefix';
      prefixFallbacks.push({
        name,
        sourceRegionCode,
        regionCode,
        sidoName: backendSido.regionName,
        sidoCode: backendSido.regionCode,
      });
      summary.prefix += 1;
    }
    feature.properties = createProperties(feature.properties, {
      name,
      sourceRegionCode,
      regionCode,
      regionCodes: [regionCode],
      mappingType,
    });
  }

  for (const feature of [...sidoGeoJson.features, ...sigunguGeoJson.features]) {
    const { name, sourceRegionCode, regionCodes } = feature.properties;
    if (!name || !sourceRegionCode || !Array.isArray(regionCodes) || !regionCodes.length) {
      throw new Error(`${name || '(이름 없음)'}: 최종 properties 검증 실패`);
    }
  }
  if (
    geometryBefore.sido !== geometrySnapshot(sidoGeoJson) ||
    geometryBefore.sigungu !== geometrySnapshot(sigunguGeoJson)
  ) {
    throw new Error('GeoJSON geometry가 변경되어 저장을 중단합니다.');
  }

  await Promise.all([
    writeFile(sidoPath, serializeGeoJson(sidoGeoJson)),
    writeFile(sigunguPath, serializeGeoJson(sigunguGeoJson)),
  ]);

  process.stdout.write(`시도 exact 매핑 수: ${summary.sidoExact}\n`);
  process.stdout.write(`시군구 exact 매핑 수: ${summary.exact}\n`);
  process.stdout.write(`시군구 prefix fallback 수: ${summary.prefix}\n`);
  process.stdout.write(`group 매핑 수: ${summary.group}\n`);
  process.stdout.write(`화성시 group 코드: ${GROUP_REGION_OVERRIDES[31240].regionCodes.join(', ')}\n`);
  for (const item of prefixFallbacks) {
    process.stdout.write(
      `[prefix] ${item.name} | ${item.sourceRegionCode} -> ${item.regionCode} | ${item.sidoName}(${item.sidoCode})\n`,
    );
  }
  const backendRegionByCode = new Map(
    backend.sigungu.map((region) => [region.regionCode, region]),
  );
  for (const item of prefixFallbacks) {
    const collision = backendRegionByCode.get(item.regionCode);
    if (collision) {
      process.stdout.write(
        `[prefix-collision] ${item.name}(${item.sourceRegionCode}) -> ${item.regionCode}, init.sql의 ${collision.regionName}과 충돌\n`,
      );
    }
  }
  const backendOnly = backend.sigungu.filter(
    (region) => !usedBackendCodes.has(region.regionCode),
  );
  for (const region of backendOnly) {
    process.stdout.write(
      `[backend-only] ${region.regionName}(${region.regionCode}), 부모 ${region.parentRegionCode}\n`,
    );
  }
};

main().catch((error) => {
  process.stderr.write(`[map:sync-region-codes] ${error.message}\n`);
  process.exitCode = 1;
});
