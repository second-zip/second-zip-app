import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useSecretarySelection } from './useSecretarySelection';

const mocks = vi.hoisted(() => ({
  authStore: {
    changeCharacter: vi.fn(),
    characterType: 'CAT',
    fetchMyPage: vi.fn(),
    isAuthenticated: true,
    myPage: { characterType: 'CAT' },
  },
}));

vi.mock('@/stores/auth', () => ({ useAuthStore: () => mocks.authStore }));

const mountSelection = () =>
  mount({
    setup: () => useSecretarySelection(),
    template: '<div />',
  });

describe('useSecretarySelection', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    mocks.authStore.characterType = 'CAT';
    mocks.authStore.isAuthenticated = true;
    mocks.authStore.myPage = { characterType: 'CAT' };
    mocks.authStore.fetchMyPage.mockResolvedValue({ characterType: 'WOMAN' });
    mocks.authStore.changeCharacter.mockImplementation(async (characterType) => {
      mocks.authStore.myPage = { characterType };
    });
  });

  afterEach(() => vi.useRealTimers());

  it('인증 상태와 저장된 프로필에 따라 비서를 초기화한다', async () => {
    mocks.authStore.myPage = null;
    const wrapper = mountSelection();
    await flushPromises();

    expect(mocks.authStore.fetchMyPage).toHaveBeenCalledOnce();
    expect(wrapper.vm.selectedCharacter).toBe('WOMAN');
  });

  it('비로그인은 회원정보를 조회하지 않는다', async () => {
    mocks.authStore.isAuthenticated = false;
    mocks.authStore.myPage = null;
    mountSelection();
    await flushPromises();

    expect(mocks.authStore.fetchMyPage).not.toHaveBeenCalled();
  });

  it('이미 선택된 비서는 API 요청 없이 안내 메시지를 표시한다', async () => {
    const wrapper = mountSelection();
    await flushPromises();

    await wrapper.vm.selectSecretary('CAT');

    expect(mocks.authStore.changeCharacter).not.toHaveBeenCalled();
    expect(wrapper.vm.message).toContain('냥냥이를 비서로');
  });

  it('선택한 비서를 저장하고 바운스 애니메이션을 종료한다', async () => {
    const wrapper = mountSelection();
    await flushPromises();

    await wrapper.vm.selectSecretary('MAN');
    expect(mocks.authStore.changeCharacter).toHaveBeenCalledWith('MAN');
    expect(wrapper.vm.selectedCharacter).toBe('MAN');
    expect(wrapper.vm.message).toContain('위장남사친을 비서로');

    await vi.runAllTimersAsync();
    expect(wrapper.vm.animatingCharacter).toBeNull();
  });

  it('저장 실패 시 이전 선택으로 되돌린다', async () => {
    mocks.authStore.changeCharacter.mockRejectedValue({
      response: { data: { message: '저장 실패' } },
    });
    const wrapper = mountSelection();
    await flushPromises();

    await wrapper.vm.selectSecretary('WOMAN');

    expect(wrapper.vm.selectedCharacter).toBe('CAT');
    expect(wrapper.vm.message).toBe('저장 실패');
    expect(wrapper.vm.saving).toBe(false);
  });

  it('준비 중 카드 메시지를 표시한다', async () => {
    const wrapper = mountSelection();
    await flushPromises();

    wrapper.vm.showPreparingMessage();

    expect(wrapper.vm.message).toContain('준비 중');
  });
});
