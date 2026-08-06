import { toReportTimestamp } from './date';

export const formatReportAddress = (report = {}) =>
  [report.roadAddress, report.detailAddress].filter(Boolean).join(' ') || '-';

export const sortReportsByFavorite = (reports = []) =>
  [...reports].sort((a, b) => {
    const favoriteOrder =
      Number(Boolean(b.favorite)) - Number(Boolean(a.favorite));

    if (favoriteOrder !== 0) return favoriteOrder;

    const dateField = a.favorite ? 'favoritedAt' : 'createdAt';

    return (
      toReportTimestamp(b[dateField]) - toReportTimestamp(a[dateField])
    );
  });

export const updateFavoriteReport = (
  reports,
  target,
  favoritedAt = new Date().toISOString(),
) =>
  sortReportsByFavorite(
    reports.map((report) =>
      report.analysisReportId === target.analysisReportId
        ? {
            ...report,
            favorite: !target.favorite,
            favoritedAt: target.favorite ? null : favoritedAt,
          }
        : report,
    ),
  );
