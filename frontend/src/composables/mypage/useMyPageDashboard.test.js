import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { useMyPageDashboard } from './useMyPageDashboard';

const mocks = vi.hoisted(() => ({
  authStore: {
    characterType: 'CAT',
    fetchMyPage: vi.fn(),
    myPage: { email: 'user@example.com' },
  },
  getReports: vi.fn(),
}));

vi.mock('@/api/report', () => ({ getReports: mocks.getReports }));
vi.mock('@/stores/auth', () => ({ useAuthStore: () => mocks.authStore }));

const setup = () => {
  let state;
  const wrapper = mount({
    setup() {
      state = useMyPageDashboard();
      return () => null;
    },
  });
  return { state, wrapper };
};

describe('useMyPageDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authStore.fetchMyPage.mockResolvedValue(undefined);
    mocks.getReports.mockResolvedValue({
      totalCount: 2,
      reports: [{ result: 'SAFE' }, { result: 'DANGER' }],
    });
  });

  test('계정과 리포트를 함께 불러와 활동을 집계한다', async () => {
    const { state } = setup();

    await flushPromises();

    expect(state.activity.value).toEqual({
      total: 2, safe: 1, caution: 0, danger: 1,
    });
    expect(state.activityLoading.value).toBe(false);
    expect(state.secretaryLabel.value).toBeTruthy();
  });

  test('계정과 리포트 조회 실패를 사용자 메시지로 표시한다', async () => {
    mocks.authStore.fetchMyPage.mockRejectedValue({
      response: { data: { message: '계정 조회 실패' } },
    });
    mocks.getReports.mockRejectedValue({
      response: { data: { message: '리포트 조회 실패' } },
    });
    const { state } = setup();

    await flushPromises();

    expect(state.errorMessage.value).toBe('계정 조회 실패');
    expect(state.activityLoading.value).toBe(false);
  });

  test('계정 조회가 성공하고 리포트만 실패하면 리포트 오류를 표시한다', async () => {
    mocks.getReports.mockRejectedValue({
      response: { data: { message: '리포트 조회 실패' } },
    });
    const { state } = setup();

    await flushPromises();

    expect(state.errorMessage.value).toBe('리포트 조회 실패');
  });
});
