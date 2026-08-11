import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import LoginView from './LoginView.vue';

const mocks = vi.hoisted(() => ({
  authStore: {
    loading: false,
    login: vi.fn(),
  },
  replace: vi.fn(),
  push: vi.fn(),
  route: { query: {} },
}));

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => mocks.authStore,
}));
vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => ({ replace: mocks.replace, push: mocks.push }),
}));

describe('LoginView 로그인 화면', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authStore.loading = false;
    mocks.route.query = {};
  });

  it('입력한 계정으로 로그인하고 메인 화면으로 이동한다', async () => {
    mocks.authStore.login.mockResolvedValue({ accountId: 1 });
    const wrapper = mount(LoginView);

    await wrapper.get('#email').setValue('user@example.com');
    await wrapper.get('#password').setValue('Password1!');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(mocks.authStore.login).toHaveBeenCalledWith({
      email: 'user@example.com',
      password: 'Password1!',
    });
    expect(mocks.replace).toHaveBeenCalledWith('/');
  });

  it('로그인 실패 메시지를 화면에 표시하고 이동하지 않는다', async () => {
    mocks.authStore.login.mockRejectedValue({
      response: { data: { message: '이메일 또는 비밀번호가 올바르지 않습니다.' } },
    });
    const wrapper = mount(LoginView);

    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.get('.error-message').text()).toBe(
      '이메일 또는 비밀번호가 올바르지 않습니다.',
    );
    expect(mocks.replace).not.toHaveBeenCalled();
  });

  it('로그인 처리 중에는 버튼을 비활성화하고 문구를 변경한다', () => {
    mocks.authStore.loading = true;

    const wrapper = mount(LoginView);
    const button = wrapper.get('button[type="submit"]');

    expect(button.attributes('disabled')).toBeDefined();
    expect(button.text()).toBe('로그인 중...');
  });

  it('로그인 후 전달받은 내부 경로로 이동한다', async () => {
    mocks.route.query = { redirect: '/mypage#ai-secretary' };
    mocks.authStore.login.mockResolvedValue({ accountId: 1 });
    const wrapper = mount(LoginView);

    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(mocks.replace).toHaveBeenCalledWith('/mypage#ai-secretary');
  });

  it('회원가입 버튼을 누르면 회원가입 화면으로 이동한다', async () => {
    const wrapper = mount(LoginView);

    await wrapper.get('button[type="button"]').trigger('click');

    expect(mocks.push).toHaveBeenCalledWith('/signup');
  });
});
