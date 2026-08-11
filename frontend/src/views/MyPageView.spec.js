import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import MyPageView from './MyPageView.vue';

const authStore = vi.hoisted(() => ({
  fetchMyPage: vi.fn(),
  changeCharacter: vi.fn(),
  logout: vi.fn(),
  myPageLoading: false,
  loading: false,
  myPage: {
    email: 'user@example.com',
    nickname: 'tester',
    characterType: 'CAT',
  },
}));

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => authStore,
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: vi.fn() }),
}));

describe('MyPageView character selection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authStore.myPage = {
      email: 'user@example.com',
      nickname: 'tester',
      characterType: 'CAT',
    };
    authStore.fetchMyPage.mockResolvedValue(authStore.myPage);
  });

  it('AI 비서 선택 영역에 직접 이동할 수 있는 앵커를 제공한다', async () => {
    const wrapper = mount(MyPageView);

    await flushPromises();

    expect(wrapper.get('#ai-secretary').text()).toContain('AI 비서 선택하기');
  });

  it('does not request a change when the current character is selected', async () => {
    const wrapper = mount(MyPageView);
    await flushPromises();
    const characterButtons = wrapper.findAll('fieldset button');

    await characterButtons[2].trigger('click');

    expect(authStore.changeCharacter).not.toHaveBeenCalled();
  });

  it('prevents duplicate character requests while a change is pending', async () => {
    let resolveChange;
    authStore.changeCharacter.mockImplementation(
      () => new Promise((resolve) => { resolveChange = resolve; }),
    );
    const wrapper = mount(MyPageView);
    await flushPromises();
    const womanButton = wrapper.findAll('fieldset button')[0];

    await womanButton.trigger('click');
    await womanButton.trigger('click');

    expect(authStore.changeCharacter).toHaveBeenCalledOnce();
    expect(authStore.changeCharacter).toHaveBeenCalledWith('WOMAN');

    resolveChange({ ...authStore.myPage, characterType: 'WOMAN' });
    await flushPromises();
    expect(wrapper.get('fieldset').attributes('disabled')).toBeUndefined();
  });
});
