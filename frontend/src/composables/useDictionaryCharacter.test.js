import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { logger } from '@/utils/logger';
import { useDictionaryCharacter } from './useDictionaryCharacter';

const mocks = vi.hoisted(() => ({
  authStore: {
    isAuthenticated: false,
    myPage: null,
    fetchMyPage: vi.fn(),
  },
}));

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => mocks.authStore,
}));
vi.mock('@/utils/logger', () => ({
  logger: { error: vi.fn() },
}));

const HostComponent = defineComponent({
  setup() {
    return useDictionaryCharacter();
  },
  template: '<div>{{ characterKey }}</div>',
});

describe('useDictionaryCharacter', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authStore.isAuthenticated = false;
    mocks.authStore.myPage = null;
    mocks.authStore.fetchMyPage.mockResolvedValue(null);
  });

  it('비로그인 상태에서는 CAT 기본값을 사용하고 회원정보를 조회하지 않는다', () => {
    const wrapper = mount(HostComponent);

    expect(wrapper.text()).toBe('cat');
    expect(mocks.authStore.fetchMyPage).not.toHaveBeenCalled();
  });

  it('로그인 사용자 정보가 없으면 회원정보를 조회한다', async () => {
    mocks.authStore.isAuthenticated = true;
    mount(HostComponent);
    await flushPromises();

    expect(mocks.authStore.fetchMyPage).toHaveBeenCalledOnce();
    expect(logger.error).not.toHaveBeenCalled();
  });

  it('회원정보 조회 실패 시 원본 오류를 기록하고 CAT fallback을 유지한다', async () => {
    const error = new Error('user request failed');
    mocks.authStore.isAuthenticated = true;
    mocks.authStore.fetchMyPage.mockRejectedValue(error);
    const wrapper = mount(HostComponent);
    await flushPromises();

    expect(logger.error).toHaveBeenCalledWith('dictionary.fetch-user', error);
    expect(wrapper.text()).toBe('cat');
  });
});
