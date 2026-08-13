import { ref } from 'vue';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { useChecklistDetail } from './useChecklistDetail';

const mocks = vi.hoisted(() => ({
  getChecklist: vi.fn(), resetChecklist: vi.fn(), toggleItem: vi.fn(),
}));

vi.mock('@/api/checklist', () => ({
  getChecklist: mocks.getChecklist,
  resetChecklist: mocks.resetChecklist,
  toggleChecklistItem: mocks.toggleItem,
}));

const response = {
  detailAddress: '101호',
  roadAddress: '서울시 마포구',
  items: [
    {
      category: 'TYPE', checked: true, checklistItemId: 1,
      contents: '유형별 항목', description: '설명 1',
    },
    {
      category: 'COMMON', checked: false, checklistItemId: 2,
      contents: '공통 항목', description: '설명 2',
    },
  ],
};

describe('useChecklistDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getChecklist.mockResolvedValue(response);
    mocks.toggleItem.mockResolvedValue(undefined);
    mocks.resetChecklist.mockResolvedValue(undefined);
  });

  test('상세 응답의 주소와 data.items를 화면 모델로 변환한다', async () => {
    const state = useChecklistDetail(ref(8));
    await state.fetchChecklist();

    expect(mocks.getChecklist).toHaveBeenCalledWith(8);
    expect(state.address.value).toBe('서울시 마포구 101호');
    expect(state.items.value[0]).toMatchObject({
      id: 1, title: '유형별 항목', description: '설명 1', checked: true,
    });
    expect(state.completedCount.value).toBe(1);
    expect(state.progress.value).toBe(50);
  });

  test('상세 조회 실패와 items 누락을 안전하게 처리한다', async () => {
    mocks.getChecklist.mockRejectedValueOnce({
      response: { data: { message: '조회 실패' } },
    });
    const failed = useChecklistDetail(8);
    await failed.fetchChecklist();
    expect(failed.loadErrorMessage.value).toBe('조회 실패');

    mocks.getChecklist.mockResolvedValueOnce({ roadAddress: '서울' });
    const empty = useChecklistDetail(8);
    await empty.fetchChecklist();
    expect(empty.items.value).toEqual([]);
    expect(empty.progress.value).toBe(0);
  });

  test('항목을 서버 상태의 반대 값으로 변경하고 진행률을 갱신한다', async () => {
    const state = useChecklistDetail(8);
    await state.fetchChecklist();

    await state.toggleItem(2);

    expect(mocks.toggleItem).toHaveBeenCalledWith(8, 2, true);
    expect(state.items.value[1].checked).toBe(true);
    expect(state.completedCount.value).toBe(2);
    expect(state.progress.value).toBe(100);
  });

  test('항목 변경의 중복 요청을 막고 실패 시 기존 값을 유지한다', async () => {
    mocks.toggleItem.mockRejectedValue({
      response: { data: { message: '변경 실패' } },
    });
    const state = useChecklistDetail(8);
    await state.fetchChecklist();
    state.pendingItemIds.value = [2];
    await state.toggleItem(2);
    expect(mocks.toggleItem).not.toHaveBeenCalled();

    state.pendingItemIds.value = [];
    await state.toggleItem(2);
    expect(state.items.value[1].checked).toBe(false);
    expect(state.actionErrorMessage.value).toBe('변경 실패');
  });

  test('초기화 성공 시 모든 항목을 해제하고 요청 중 중복을 막는다', async () => {
    const state = useChecklistDetail(8);
    await state.fetchChecklist();
    await state.resetItems();

    expect(mocks.resetChecklist).toHaveBeenCalledWith(8);
    expect(state.items.value.every((item) => !item.checked)).toBe(true);

    state.pendingItemIds.value = [2];
    await state.resetItems();
    expect(mocks.resetChecklist).toHaveBeenCalledTimes(1);
  });
});
