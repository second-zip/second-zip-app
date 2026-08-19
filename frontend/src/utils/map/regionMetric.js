import {
  DAMAGE_COLOR_SCALE,
  JEONSE_COLOR_SCALE,
  NO_DATA_COLOR,
} from '@/constants/map/regionMap';
import { getGroupRegionRows } from '@/utils/map/regionData';

const noDataMetric = () => ({
  metricValue: null,
  displayValue: '-',
  hasData: false,
});

const formatChangeRate = (value) => `${Number(value.toFixed(2))}%`;

export const getGroupMetric = (group, dataMap, dataType) => {
  const rows = getGroupRegionRows(group, dataMap);
  if (!rows.length) return noDataMetric();

  if (dataType === 'fraud-damage') {
    const metricValue = rows.reduce(
      (sum, row) => sum + Number(row.damageHouseCount ?? 0),
      0,
    );
    return { metricValue, displayValue: `${metricValue}건`, hasData: true };
  }

  const values = rows
    .map((row) => row.changeRate)
    .filter((value) => value != null && value !== '')
    .map(Number)
    .filter(Number.isFinite);
  if (!values.length) return noDataMetric();
  const metricValue =
    values.reduce((sum, value) => sum + value, 0) / values.length;
  return {
    metricValue,
    displayValue: formatChangeRate(metricValue),
    hasData: true,
  };
};

export const getMetricColorIndex = (value, metricValues) => {
  const values = metricValues.filter(Number.isFinite).sort((a, b) => a - b);
  if (!values.length || values[0] === values.at(-1)) return 2;

  const thresholds = Array.from(
    { length: 5 },
    (_, index) => values[Math.ceil((values.length * (index + 1)) / 6) - 1],
  );
  return thresholds.reduce(
    (colorIndex, threshold) => colorIndex + Number(value > threshold),
    0,
  );
};

export const getRegionFill = (item, metricValues, dataType) => {
  if (!item.hasData) return NO_DATA_COLOR;
  const colors =
    dataType === 'fraud-damage' ? DAMAGE_COLOR_SCALE : JEONSE_COLOR_SCALE;
  const riskValue =
    dataType === 'price-index'
      ? Math.abs(item.metricValue)
      : item.metricValue;
  const riskMetricValues =
    dataType === 'price-index'
      ? metricValues.map((value) => Math.abs(value))
      : metricValues;

  return colors[getMetricColorIndex(riskValue, riskMetricValues)];
};
