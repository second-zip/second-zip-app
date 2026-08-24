import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import SecretaryChangeView from './SecretaryChangeView.vue';

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

describe('SecretaryChangeView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authStore.characterType = 'CAT';
    mocks.authStore.myPage = { characterType: 'CAT' };
    mocks.authStore.changeCharacter.mockResolvedValue({ characterType: 'MAN' });
  });

  it('비서 선택지를 표시하고 선택을 저장한다', async () => {
    const wrapper = mount(SecretaryChangeView);
    await flushPromises();
    const cards = wrapper.findAll('.secretary-card');

    expect(cards).toHaveLength(4);
    expect(cards[0].classes()).toContain('selected');

    await cards[2].trigger('click');
    await flushPromises();

    expect(mocks.authStore.changeCharacter).toHaveBeenCalledWith('MAN');
    expect(wrapper.get('[role="status"]').text()).toContain('위장남사친');
  });

  it('준비 중 비서를 누르면 안내한다', async () => {
    const wrapper = mount(SecretaryChangeView);

    await wrapper.findAll('.secretary-card')[3].trigger('click');

    expect(wrapper.get('[role="status"]').text()).toContain('준비 중');
  });
});
