import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import SignupView from './SignupView.vue';

const mocks = vi.hoisted(() => ({
  authStore: {
    signup: vi.fn(),
  },
  replace: vi.fn(),
}));

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => mocks.authStore,
}));
vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: mocks.replace }),
}));

const mountView = () =>
  mount(SignupView, {
    global: {
      stubs: {
        BottomSheetLayout: {
          template: '<section><slot name="header"/><slot/></section>',
        },
        DefaultSheetHeader: {
          template: '<header />',
        },
      },
    },
  });

const fillValidForm = async (wrapper) => {
  await wrapper.get('#nickname').setValue('길동');
  await wrapper.get('#email').setValue('user@example.com');
  await wrapper.get('#password').setValue('Password1!');
  await wrapper.get('#passwordConfirm').setValue('Password1!');
};

describe('SignupView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('네 개의 회원가입 입력 필드를 렌더링한다', () => {
    const wrapper = mountView();

    expect(wrapper.findAll('.auth-input')).toHaveLength(4);
    expect(wrapper.findAll('label').map((label) => label.text())).toEqual([
      '닉네임',
      '이메일',
      '비밀번호',
      '비밀번호 확인',
    ]);
  });

  it('유효하지 않은 폼은 제출하지 않고 검증 메시지를 표시한다', async () => {
    const wrapper = mountView();

    await wrapper.get('form').trigger('submit');

    expect(mocks.authStore.signup).not.toHaveBeenCalled();
    expect(wrapper.get('.error-message').text()).toBe('입력값을 다시 확인해 주세요.');
    expect(wrapper.findAll('.auth-input__status-message-wrong')).toHaveLength(4);
  });

  it('유효한 폼으로 회원가입하고 로그인 화면으로 이동한다', async () => {
    mocks.authStore.signup.mockResolvedValue({ accountId: 1 });
    const wrapper = mountView();
    await fillValidForm(wrapper);

    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(mocks.authStore.signup).toHaveBeenCalledWith({
      characterType: 'CAT',
      email: 'user@example.com',
      password: 'Password1!',
      passwordConfirm: 'Password1!',
      nickname: '길동',
      termConsents: [
        { agreed: true, termId: 1 },
        { agreed: true, termId: 2 },
      ],
    });
    expect(mocks.replace).toHaveBeenCalledWith('/login');
  });

  it('회원가입 API 오류 메시지를 표시하고 이동하지 않는다', async () => {
    mocks.authStore.signup.mockRejectedValue({
      response: { data: { message: '이미 사용 중인 이메일입니다.' } },
    });
    const wrapper = mountView();
    await fillValidForm(wrapper);

    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.get('.error-message').text()).toBe('이미 사용 중인 이메일입니다.');
    expect(mocks.replace).not.toHaveBeenCalled();
  });
});
