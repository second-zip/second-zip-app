import { describe, expect, test } from 'vitest';

import {
  formatReportAddress,
  sortReportsByFavorite,
  updateFavoriteReport,
} from './list';

const reports = [
  { analysisReportId: 1, favorite: false, createdAt: '2026-08-01' },
  { analysisReportId: 4, favorite: false, createdAt: '2026-08-02' },
  {
    analysisReportId: 2,
    favorite: true,
    favoritedAt: '2026-08-02',
  },
  {
    analysisReportId: 3,
    favorite: true,
    favoritedAt: '2026-08-03',
  },
];

describe('report list utils', () => {
  test('도로명 주소와 상세 주소를 결합하고 빈 주소는 대체한다', () => {
    expect(
      formatReportAddress({ roadAddress: '서울시 송파구', detailAddress: '101호' }),
    ).toBe('서울시 송파구 101호');
    expect(formatReportAddress()).toBe('-');
  });

  test('즐겨찾기와 날짜의 내림차순으로 새 배열을 정렬한다', () => {
    const sorted = sortReportsByFavorite(reports);

    expect(sorted.map(({ analysisReportId }) => analysisReportId)).toEqual([
      3, 2, 4, 1,
    ]);
    expect(sorted).not.toBe(reports);
  });

  test('즐겨찾기 상태와 시각을 새 객체로 갱신한다', () => {
    const favorited = updateFavoriteReport(
      reports,
      reports[0],
      '2026-08-04T10:00:00',
    );
    const unfavorited = updateFavoriteReport(
      favorited,
      favorited.find(({ analysisReportId }) => analysisReportId === 3),
    );

    expect(favorited[0]).toMatchObject({
      analysisReportId: 1,
      favorite: true,
      favoritedAt: '2026-08-04T10:00:00',
    });
    expect(unfavorited.find(({ analysisReportId }) => analysisReportId === 3))
      .toMatchObject({ favorite: false, favoritedAt: null });
    expect(reports[0].favorite).toBe(false);
  });
});
