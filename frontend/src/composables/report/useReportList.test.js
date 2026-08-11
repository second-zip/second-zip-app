import { beforeEach, describe, expect, test, vi } from 'vitest';

import { useReportList } from './useReportList';

const mocks = vi.hoisted(() => ({
  addFavorite: vi.fn(),
  deleteReport: vi.fn(),
  getReports: vi.fn(),
  loggerError: vi.fn(),
  removeFavorite: vi.fn(),
}));

vi.mock('@/api/report', () => ({
  addReportFavorite: mocks.addFavorite,
  deleteReport: mocks.deleteReport,
  deleteReportFavorite: mocks.removeFavorite,
  getReports: mocks.getReports,
}));
vi.mock('@/utils/logger', () => ({
  logger: { error: mocks.loggerError },
}));

const report = {
  analysisReportId: 1,
  favorite: false,
  createdAt: '2026-08-01',
};

describe('useReportList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getReports.mockResolvedValue({ reports: [report] });
    mocks.addFavorite.mockResolvedValue(undefined);
    mocks.removeFavorite.mockResolvedValue(undefined);
    mocks.deleteReport.mockResolvedValue(undefined);
  });

  test('목록 조회 상태와 응답을 관리한다', async () => {
    const list = useReportList();
    const request = list.fetchReports();

    expect(list.isLoading.value).toBe(true);
    await request;
    expect(list.reports.value).toEqual([report]);
    expect(list.isLoading.value).toBe(false);
  });

  test('목록 조회 오류와 비배열 응답을 안전하게 처리한다', async () => {
    mocks.getReports.mockRejectedValueOnce({
      response: { data: { message: '조회 실패' } },
    });
    const failed = useReportList();
    await failed.fetchReports();
    expect(failed.errorMessage.value).toBe('조회 실패');

    mocks.getReports.mockResolvedValueOnce({ reports: null });
    const empty = useReportList();
    await empty.fetchReports();
    expect(empty.reports.value).toEqual([]);
  });

  test('즐겨찾기 추가와 해제 성공 시 상태를 갱신한다', async () => {
    const list = useReportList();
    list.reports.value = [report];

    await list.toggleFavorite(report);
    expect(mocks.addFavorite).toHaveBeenCalledWith(1);
    expect(list.reports.value[0].favorite).toBe(true);

    await list.toggleFavorite(list.reports.value[0]);
    expect(mocks.removeFavorite).toHaveBeenCalledWith(1);
    expect(list.reports.value[0].favorite).toBe(false);
  });

  test('즐겨찾기 실패 시 기존 상태를 유지하고 기록한다', async () => {
    mocks.addFavorite.mockRejectedValueOnce(new Error('fail'));
    const list = useReportList();
    list.reports.value = [report];

    await list.toggleFavorite(report);
    expect(list.reports.value[0].favorite).toBe(false);
    expect(mocks.loggerError).toHaveBeenCalled();
  });

  test('삭제 성공·실패·중복 요청 상태를 관리한다', async () => {
    const list = useReportList();
    list.reports.value = [report];
    expect(await list.removeReport(null)).toBe(false);

    await expect(list.removeReport(report)).resolves.toBe(true);
    expect(list.reports.value).toEqual([]);

    mocks.deleteReport.mockRejectedValueOnce({
      response: { data: { message: '삭제 실패' } },
    });
    await expect(list.removeReport(report)).resolves.toBe(false);
    expect(list.deleteErrorMessage.value).toBe('삭제 실패');
    list.resetDeleteError();
    expect(list.deleteErrorMessage.value).toBe('');
  });
});
