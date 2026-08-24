import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useProfileEditForm } from './useProfileEditForm';

const mocks = vi.hoisted(() => ({
  authStore: {
    changePassword: vi.fn(),
    fetchMyPage: vi.fn(),
    myPage: null,
    updateProfile: vi.fn(),
  },
  replace: vi.fn(),
}));

vi.mock('@/stores/auth', () => ({ useAuthStore: () => mocks.authStore }));
vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: mocks.replace }),
}));

const mountForm = () =>
  mount({
    setup: () => useProfileEditForm(),
    template: '<div />',
  });

describe('useProfileEditForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authStore.myPage = null;
    mocks.authStore.fetchMyPage.mockResolvedValue({
      email: 'user@example.com',
      nickname: '기존닉네임',
    });
    mocks.authStore.updateProfile.mockResolvedValue({});
    mocks.authStore.changePassword.mockResolvedValue({});
  });

  it('마운트 시 회원정보를 조회해 닉네임을 초기화한다', async () => {
    const wrapper = mountForm();
    await flushPromises();

    expect(mocks.authStore.fetchMyPage).toHaveBeenCalledOnce();
    expect(wrapper.vm.nickname).toBe('기존닉네임');
  });

  it('스토어에 회원정보가 있으면 재조회하지 않는다', async () => {
    mocks.authStore.myPage = {
      email: 'saved@example.com',
      nickname: '저장닉네임',
    };

    const wrapper = mountForm();
    await flushPromises();

    expect(mocks.authStore.fetchMyPage).not.toHaveBeenCalled();
    expect(wrapper.vm.email).toBe('saved@example.com');
    expect(wrapper.vm.nickname).toBe('저장닉네임');
  });

  it('닉네임과 비밀번호 입력을 단계별로 검증한다', async () => {
    const wrapper = mountForm();
    await flushPromises();

    wrapper.vm.nickname = '한';
    await wrapper.vm.saveAccount();
    expect(wrapper.vm.formMessage).toContain('닉네임은');

    wrapper.vm.nickname = '새닉네임';
    await wrapper.vm.saveAccount();
    expect(wrapper.vm.formMessage).toContain('현재 비밀번호');

    wrapper.vm.passwords.currentPassword = 'password1!';
    wrapper.vm.passwords.newPassword = 'invalid';
    await wrapper.vm.saveAccount();
    expect(wrapper.vm.formMessage).toContain('영문, 숫자, 특수문자');
    expect(mocks.authStore.updateProfile).not.toHaveBeenCalled();
  });

  it('프로필과 비밀번호를 순서대로 변경한 뒤 로그인으로 이동한다', async () => {
    const wrapper = mountForm();
    await flushPromises();
    wrapper.vm.nickname = '  새닉네임  ';
    wrapper.vm.passwords.currentPassword = 'password1!';
    wrapper.vm.passwords.newPassword = 'newPassword1!';

    await wrapper.vm.saveAccount();

    expect(mocks.authStore.updateProfile).toHaveBeenCalledWith('새닉네임');
    expect(mocks.authStore.changePassword).toHaveBeenCalledWith({
      currentPassword: 'password1!',
      newPassword: 'newPassword1!',
      newPasswordConfirm: 'newPassword1!',
    });
    expect(mocks.replace).toHaveBeenCalledWith({
      name: 'login',
      query: { accountUpdated: 'true' },
    });
    expect(wrapper.vm.submitting).toBe(false);
  });

  it('저장 실패 메시지를 표시하고 submitting을 복원한다', async () => {
    mocks.authStore.updateProfile.mockRejectedValue({
      response: { data: { message: '변경 실패' } },
    });
    const wrapper = mountForm();
    await flushPromises();
    wrapper.vm.nickname = '새닉네임';
    wrapper.vm.passwords.currentPassword = 'password1!';
    wrapper.vm.passwords.newPassword = 'newPassword1!';

    await wrapper.vm.saveAccount();

    expect(wrapper.vm.formMessage).toBe('변경 실패');
    expect(wrapper.vm.submitting).toBe(false);
  });
});
