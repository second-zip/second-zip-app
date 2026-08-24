import { mount, RouterLinkStub } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

import WordDictionaryView from './WordDictionaryView.vue';

const authStore = vi.hoisted(() => ({
  isAuthenticated: false,
  myPage: null,
  fetchMyPage: vi.fn(),
}));

vi.mock('@/stores/auth', () => ({ useAuthStore: () => authStore }));

describe('WordDictionaryView', () => {
  it('전세 용어 목록과 비서 안내를 표시한다', () => {
    const wrapper = mount(WordDictionaryView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    });

    expect(wrapper.get('.word-header').text()).toContain('전세사기 도감');
    expect(wrapper.findAll('.word-item').length).toBeGreaterThan(5);
    expect(wrapper.text()).toContain('대항력');
    expect(wrapper.get('.secretary-guide').exists()).toBe(true);
  });
});
