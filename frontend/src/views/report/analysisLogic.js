export const selectSecretaryValue = (
  values,
  secretary,
  fallbackSecretary = 'cat',
) => values[secretary] ?? values[fallbackSecretary];

export const formatKoreanDeposit = (value) => {
  const amount = Number(value) || 0;
  const eok = Math.floor(amount / 100_000_000);
  const man = Math.floor((amount % 100_000_000) / 10_000);
  const parts = [];

  if (eok > 0) parts.push(`${eok.toLocaleString('ko-KR')}억`);
  if (man > 0) parts.push(`${man}만`);

  return parts.join(' ') || '0';
};
