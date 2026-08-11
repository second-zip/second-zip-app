import { beforeEach, describe, expect, test, vi } from 'vitest';

import api from './instance';
import {
  addReportFavorite,
  createReport,
  deleteReport,
  deleteReportFavorite,
  generateAiMessages,
  getReport,
  getReports,
  getSharedReport,
  shareReport,
} from './report';

vi.mock('./instance', () => ({
  default: { get: vi.fn(), post: vi.fn(), delete: vi.fn() },
}));

describe('report API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.get.mockResolvedValue({ data: { ok: true } });
    api.post.mockResolvedValue({ data: { ok: true } });
    api.delete.mockResolvedValue({ data: { ok: true } });
  });

  test.each([
    [getReports, [], '/analysis-reports'],
    [getReport, [7], '/analysis-reports/7'],
    [getSharedReport, ['token'], '/shared/token'],
  ])('GET 요청의 data를 반환한다', async (request, args, url) => {
    await expect(request(...args)).resolves.toEqual({ ok: true });
    expect(api.get).toHaveBeenCalledWith(url);
  });

  test.each([
    [addReportFavorite, [7], '/analysis-reports/7/favorite', undefined],
    [createReport, [{ deposit: 1 }], '/analysis-reports/analyze', { deposit: 1 }],
    [generateAiMessages, [7], '/analysis-reports/7/ai-generate-messages', undefined],
    [shareReport, [7], '/analysis-reports/7/share', undefined],
  ])('POST 요청의 data를 반환한다', async (request, args, url, body) => {
    await expect(request(...args)).resolves.toEqual({ ok: true });
    expect(api.post).toHaveBeenCalledWith(url, ...(body ? [body] : []));
  });

  test.each([
    [deleteReportFavorite, '/analysis-reports/7/favorite'],
    [deleteReport, '/analysis-reports/7'],
  ])('DELETE 요청의 data를 반환한다', async (request, url) => {
    await expect(request(7)).resolves.toEqual({ ok: true });
    expect(api.delete).toHaveBeenCalledWith(url);
  });
});
