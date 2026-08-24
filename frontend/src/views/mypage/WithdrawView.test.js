import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import WithdrawView from './WithdrawView.vue';

const mocks = vi.hoisted(() => ({
  authStore: { withdraw: vi.fn() },
  back: vi.fn(),
  replace: vi.fn(),
}));

vi.mock('@/stores/auth', () => ({ useAuthStore: () => mocks.authStore }));
vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: mocks.replace }),
}));

const mountView = () =>
  mount(WithdrawView, {
    global: { mocks: { $router: { back: mocks.back } } },
  });

describe('WithdrawView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authStore.withdraw.mockResolvedValue({});
  });

  it('비밀번호와 동의가 없으면 탈퇴를 요청하지 않는다', async () => {
    const wrapper = mountView();

    await wrapper.get('form').trigger('submit');

    expect(wrapper.get('[role="alert"]').text()).toContain('탈퇴 동의');
    expect(mocks.authStore.withdraw).not.toHaveBeenCalled();
  });

  it('탈퇴 성공 후 메인으로 이동한다', async () => {
    const wrapper = mountView();
    await wrapper.get('input[type="password"]').setValue('password1!');
    await wrapper.get('input[type="checkbox"]').setValue(true);

    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(mocks.authStore.withdraw).toHaveBeenCalledWith('password1!');
    expect(mocks.replace).toHaveBeenCalledWith({ name: 'main' });
  });

  it('탈퇴 실패 메시지를 표시한다', async () => {
    mocks.authStore.withdraw.mockRejectedValue({
      response: { data: { message: '비밀번호 불일치' } },
    });
    const wrapper = mountView();
    await wrapper.get('input[type="password"]').setValue('wrong');
    await wrapper.get('input[type="checkbox"]').setValue(true);

    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.get('[role="alert"]').text()).toBe('비밀번호 불일치');
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined();
  });

  it('뒤로 가기 버튼을 연결한다', async () => {
    const wrapper = mountView();

    await wrapper.get('.mypage-header__back').trigger('click');

    expect(mocks.back).toHaveBeenCalledOnce();
  });
});
