import { beforeEach, describe, expect, test, vi } from 'vitest';

import api from './instance';
import {
  createChecklist,
  getChecklist,
  getChecklists,
  resetChecklist,
  toggleChecklistItem,
} from './checklist';

vi.mock('./instance', () => ({
  default: { get: vi.fn(), patch: vi.fn(), post: vi.fn() },
}));

describe('checklist API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.get.mockResolvedValue({ data: { items: [] } });
    api.patch.mockResolvedValue({ data: { checked: true } });
    api.post.mockResolvedValue({ data: { reportChecklistId: 8 } });
  });

  test.each([
    [getChecklists, [], '/checklists'],
    [getChecklist, [8], '/checklists/8'],
  ])('조회 응답의 data를 반환한다', async (request, args, url) => {
    await expect(request(...args)).resolves.toEqual({ items: [] });
    expect(api.get).toHaveBeenCalledWith(url);
  });

  test('항목의 체크 상태를 PATCH 요청으로 전달한다', async () => {
    await expect(toggleChecklistItem(8, 3, true)).resolves.toEqual({
      checked: true,
    });
    expect(api.patch).toHaveBeenCalledWith('/checklists/8/items/3', {
      checked: true,
    });
  });

  test('체크리스트 초기화 API를 호출한다', async () => {
    await expect(resetChecklist(8)).resolves.toEqual({ checked: true });
    expect(api.patch).toHaveBeenCalledWith('/checklists/8/reset');
  });

  test('분석 리포트 ID로 체크리스트를 생성한다', async () => {
    await expect(createChecklist(12)).resolves.toEqual({
      reportChecklistId: 8,
    });
    expect(api.post).toHaveBeenCalledWith('/checklists/reports/12');
  });
});
