const REPORT_RESULTS = Object.freeze({
  SAFE: 'safe',
  CAUTION: 'caution',
  DANGER: 'danger',
});

export const summarizeReports = (response = {}) => {
  const reports = Array.isArray(response) ? response : response.reports ?? [];
  const summary = { total: response.totalCount ?? reports.length, safe: 0, caution: 0, danger: 0 };

  reports.forEach((report) => {
    const key = REPORT_RESULTS[String(report.result ?? '').toUpperCase()];
    if (key) summary[key] += 1;
  });

  return summary;
};

export const isValidNickname = (nickname) => {
  const length = String(nickname ?? '').trim().length;
  return length >= 2 && length <= 20;
};

export const isValidPassword = (password) =>
  /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,100}$/.test(password);

export const withObjectParticle = (word = '') => {
  const value = String(word);
  const lastCharacterCode = value.charCodeAt(value.length - 1);
  const hasFinalConsonant =
    lastCharacterCode >= 0xac00 &&
    lastCharacterCode <= 0xd7a3 &&
    (lastCharacterCode - 0xac00) % 28 !== 0;

  return `${value}${hasFinalConsonant ? '을' : '를'}`;
};
