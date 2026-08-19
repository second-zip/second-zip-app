import { describe, expect, test } from 'vitest';

import {
  formatReportDate,
  toReportDateTime,
  toReportTimestamp,
} from './date';

describe('report date', () => {
  test('ISO LocalDateTime 문자열을 화면 형식으로 변환한다', () => {
    expect(formatReportDate('2026-08-06T15:30:00')).toBe('2026. 8. 6');
    expect(toReportDateTime('2026-08-06T15:30:00')).toBe('2026-08-06');
  });

  test('배열로 직렬화된 LocalDateTime을 변환한다', () => {
    expect(formatReportDate([2026, 8, 6, 15, 30])).toBe('2026. 8. 6');
    expect(toReportDateTime([2026, 8, 6, 15, 30])).toBe('2026-08-06');
  });

  test('날짜 객체 형태와 잘못된 값을 안전하게 처리한다', () => {
    expect(
      formatReportDate({ year: 2026, monthValue: 8, dayOfMonth: 6 }),
    ).toBe('2026. 8. 6');
    expect(formatReportDate('invalid')).toBe('-');
    expect(formatReportDate(new Date(2026, 7, 6))).toBe('2026. 8. 6');
    expect(formatReportDate([2026, 13, 1])).toBe('-');
    expect(toReportDateTime(null)).toBeUndefined();
  });

  test('문자열과 배열 날짜를 정렬 가능한 시각으로 변환한다', () => {
    expect(toReportTimestamp('2026-08-06T15:30:00')).toBeGreaterThan(0);
    expect(toReportTimestamp([2026, 8, 6, 15, 30])).toBeGreaterThan(0);
    expect(
      toReportTimestamp({
        year: 2026,
        monthValue: 8,
        dayOfMonth: 6,
        hour: 15,
        minute: 30,
      }),
    ).toBeGreaterThan(0);
    expect(toReportTimestamp(null)).toBe(0);
    expect(toReportTimestamp('invalid')).toBe(0);
  });
});
