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
          props: ['titleRatio'],
          template:
            '<section class="bottom-sheet-layout" :data-title-ratio="titleRatio"><slot name="header"/><slot/></section>',
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

const agreeRequiredTerms = async (wrapper) => {
  for (const row of wrapper.findAll('.term-row')) {
    await row.trigger('click');
    await wrapper.get('.term-inline-confirm').trigger('click');
  }
};

describe('SignupView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('상단 영역과 폼 여백을 줄인 회원가입 레이아웃을 렌더링한다', () => {
    const wrapper = mountView();

    expect(
      wrapper.get('.bottom-sheet-layout').attributes('data-title-ratio'),
    ).toBe('10');
    expect(wrapper.get('.signup-box').classes()).not.toContain('gap-4');
    expect(wrapper.get('.terms-description').classes()).not.toContain('mb-3');
  });

  it('네 개의 회원가입 입력 필드를 렌더링한다', async () => {
    const wrapper = mountView();

    await flushPromises();

    expect(wrapper.findAll('.auth-input')).toHaveLength(4);
    expect(
      wrapper.findAll('.auth-input label').map((label) => label.text()),
    ).toEqual([
      '닉네임',
      '이메일',
      '비밀번호',
      '비밀번호 확인',
    ]);
  });

  it('두 개의 필수 동의 항목만 렌더링한다', async () => {
    const wrapper = mountView();

    await flushPromises();

    expect(wrapper.findAll('.term-checkbox')).toHaveLength(2);
    expect(wrapper.findAll('.term-label').map((label) => label.text())).toEqual([
      '[필수] 서비스 이용약관 동의',
      '[필수] 개인정보 수집·이용 동의',
    ]);
    expect(wrapper.find('.privacy-policy').exists()).toBe(false);
    expect(wrapper.text()).not.toContain('마케팅 정보 수신');
  });

  it('상단 체크를 누르면 동의 처리 전 약관 설명부터 펼친다', async () => {
    const wrapper = mountView();

    await flushPromises();
    await wrapper.findAll('.term-checkbox')[0].trigger('click');

    expect(wrapper.get('.term-accordion').text()).toContain(
      '제1조 목적',
    );
    expect(wrapper.findAll('.term-row')[0].attributes('aria-checked')).toBe(
      'false',
    );
    expect(wrapper.findAll('.term-row')[0].attributes('aria-expanded')).toBe(
      'true',
    );
  });

  it('펼친 약관에서 확인하면 해당 동의를 자동 선택하고 접는다', async () => {
    const wrapper = mountView();

    await flushPromises();
    await wrapper.findAll('.term-open-button')[0].trigger('click');
    await wrapper.get('.term-inline-confirm').trigger('click');

    expect(wrapper.findAll('.term-row')[0].attributes('aria-checked')).toBe(
      'true',
    );
    expect(wrapper.find('.term-accordion').exists()).toBe(false);
  });

  it('다른 약관을 열면 기존 아코디언을 닫고 선택한 약관만 표시한다', async () => {
    const wrapper = mountView();
    const rows = wrapper.findAll('.term-row');

    await rows[0].trigger('click');
    await rows[1].trigger('click');

    expect(wrapper.findAll('.term-accordion')).toHaveLength(1);
    expect(wrapper.get('.term-accordion').text()).toContain(
      '개인정보 수집·이용 동의',
    );
    expect(rows[0].attributes('aria-expanded')).toBe('false');
    expect(rows[1].attributes('aria-expanded')).toBe('true');
  });

  it('두 약관을 확인하기 전에는 계정 생성 버튼을 비활성화한다', async () => {
    const wrapper = mountView();
    const submitButton = wrapper.get('button[type="submit"]');

    expect(submitButton.attributes('disabled')).toBeDefined();

    await agreeRequiredTerms(wrapper);

    expect(submitButton.attributes('disabled')).toBeUndefined();
  });

  it('유효하지 않은 폼은 제출하지 않고 검증 메시지를 표시한다', async () => {
    const wrapper = mountView();

    await flushPromises();

    await wrapper.get('form').trigger('submit');

    expect(mocks.authStore.signup).not.toHaveBeenCalled();
    expect(wrapper.get('.error-message').text()).toBe('입력값을 다시 확인해 주세요.');
    expect(wrapper.findAll('.auth-input__status-message-wrong')).toHaveLength(4);
  });

  it('유효한 폼으로 회원가입하고 로그인 화면으로 이동한다', async () => {
    mocks.authStore.signup.mockResolvedValue({ accountId: 1 });
    const wrapper = mountView();
    await flushPromises();
    await fillValidForm(wrapper);
    await agreeRequiredTerms(wrapper);

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
    await flushPromises();
    await fillValidForm(wrapper);
    await agreeRequiredTerms(wrapper);

    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(wrapper.get('.error-message').text()).toBe('이미 사용 중인 이메일입니다.');
    expect(mocks.replace).not.toHaveBeenCalled();
  });
});
