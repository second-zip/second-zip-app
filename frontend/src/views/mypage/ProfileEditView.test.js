import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ProfileEditView from './ProfileEditView.vue';

const mocks = vi.hoisted(() => ({
  authStore: {
    changePassword: vi.fn(),
    fetchMyPage: vi.fn(),
    myPage: {
      email: 'user@example.com',
      nickname: '기존닉네임',
    },
    updateProfile: vi.fn(),
  },
  replace: vi.fn(),
}));

vi.mock('@/stores/auth', () => ({ useAuthStore: () => mocks.authStore }));
vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: mocks.replace }),
}));

describe('ProfileEditView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authStore.updateProfile.mockResolvedValue({});
    mocks.authStore.changePassword.mockResolvedValue({});
  });

  it('현재 회원정보를 표시하고 유효한 폼을 저장한다', async () => {
    const wrapper = mount(ProfileEditView);
    await flushPromises();

    expect(wrapper.text()).toContain('user@example.com');
    expect(wrapper.get('#nickname').element.value).toBe('기존닉네임');

    await wrapper.get('#nickname').setValue('새닉네임');
    await wrapper.get('#current-password').setValue('password1!');
    await wrapper.get('#new-password').setValue('newPassword1!');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(mocks.authStore.updateProfile).toHaveBeenCalledWith('새닉네임');
    expect(mocks.authStore.changePassword).toHaveBeenCalled();
    expect(mocks.replace).toHaveBeenCalledWith({
      name: 'login',
      query: { accountUpdated: 'true' },
    });
  });

  it('유효하지 않은 폼의 안내 메시지를 표시한다', async () => {
    const wrapper = mount(ProfileEditView);
    await flushPromises();

    await wrapper.get('#nickname').setValue('한');
    await wrapper.get('form').trigger('submit');

    expect(wrapper.get('[role="alert"]').text()).toContain('닉네임');
  });
});
