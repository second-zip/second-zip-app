import { beforeEach, describe, expect, test, vi } from 'vitest';

import { useChecklistList } from './useChecklistList';

const mocks = vi.hoisted(() => ({
  createChecklist: vi.fn(),
  getChecklists: vi.fn(),
}));

vi.mock('@/api/checklist', () => ({
  createChecklist: mocks.createChecklist,
  getChecklists: mocks.getChecklists,
}));

const report = {
  analysisReportId: 11,
  checklistCreated: false,
  progressPercentage: 27,
};

describe('useChecklistList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getChecklists.mockResolvedValue([report]);
    mocks.createChecklist.mockResolvedValue({ reportChecklistId: 31 });
  });

  test('체크리스트 목록 조회 결과와 로딩 상태를 관리한다', async () => {
    const state = useChecklistList();
    const request = state.fetchChecklists();

    expect(state.isLoading.value).toBe(true);
    await request;
    expect(state.checklists.value).toEqual([report]);
    expect(state.isLoading.value).toBe(false);
  });

  test('생성 성공 시 서버 ID와 초기 진행률을 목록에 반영한다', async () => {
    const state = useChecklistList();
    state.checklists.value = [report];

    const created = await state.createChecklist(report);

    expect(mocks.createChecklist).toHaveBeenCalledWith(11);
    expect(created).toMatchObject({
      checklistCreated: true,
      progressPercentage: 0,
      reportChecklistId: 31,
    });
    expect(state.checklists.value[0]).toEqual(created);
    expect(state.creatingReportIds.value).toEqual([]);
  });

  test('이미 생성됐거나 생성 중인 리포트의 중복 요청을 막는다', async () => {
    const state = useChecklistList();
    state.creatingReportIds.value = [11];

    await expect(state.createChecklist(report)).resolves.toBe(false);
    await expect(state.createChecklist({
      ...report, analysisReportId: 12, checklistCreated: true,
    })).resolves.toBe(false);
    expect(mocks.createChecklist).not.toHaveBeenCalled();
  });

  test('생성 실패 메시지를 표시하고 생성 중 상태를 해제한다', async () => {
    mocks.createChecklist.mockRejectedValue({
      response: { data: { message: '생성 실패' } },
    });
    const state = useChecklistList();

    await expect(state.createChecklist(report)).resolves.toBe(false);
    expect(state.creationErrorMessage.value).toBe('생성 실패');
    expect(state.creatingReportIds.value).toEqual([]);
  });
});
