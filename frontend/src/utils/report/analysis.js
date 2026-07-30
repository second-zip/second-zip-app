// 보증금 표시, 비서 선택, 위험도 집계 등 분석 화면의 순수 로직을 제공
export const selectSecretaryValue = (
  values,
  secretary,
  fallbackSecretary = 'cat',
) => values[secretary] ?? values[fallbackSecretary];

export const formatKoreanDeposit = (value) => {
  const amount = Number(value) || 0;
  const eok = Math.floor(amount / 100_000_000);
  const man = Math.floor((amount % 100_000_000) / 10_000);

  return `${eok.toLocaleString('ko-KR')}억 ${man}만원`;
};

export const aggregateRiskStatuses = (statuses = [], dangerThreshold = 3) => {
  if (statuses.includes('danger')) return 'danger';

  const cautionCount = statuses.filter((status) => status === 'caution').length;

  if (cautionCount >= dangerThreshold) return 'danger';
  if (cautionCount > 0) return 'caution';
  return 'safe';
};

export const getAggregateRiskStatus = (items = []) =>
  aggregateRiskStatuses(items.map(({ status }) => status));

export const toNumericAmount = (value) =>
  Number(String(value).replace(/\D/g, '')) || 0;

export const getRentRatio = (deposit, marketPrice) =>
  Math.min(Math.round((deposit / marketPrice) * 100), 999);
