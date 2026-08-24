import { mount, RouterLinkStub } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import DictionaryGuideView from './DictionaryGuideView.vue';

const mocks = vi.hoisted(() => ({
  authStore: {
    isAuthenticated: false,
    myPage: null,
    fetchMyPage: vi.fn(),
  },
  route: { meta: { guideType: 'register' } },
}));

vi.mock('@/stores/auth', () => ({ useAuthStore: () => mocks.authStore }));
vi.mock('vue-router', async (importOriginal) => ({
  ...(await importOriginal()),
  useRoute: () => mocks.route,
}));

const mountView = () =>
  mount(DictionaryGuideView, {
    global: { stubs: { RouterLink: RouterLinkStub } },
  });

describe('DictionaryGuideView', () => {
  beforeEach(() => {
    mocks.route.meta = { guideType: 'register' };
  });

  it('등기부등본 가이드의 첫 탭을 표시한다', () => {
    const wrapper = mountView();

    expect(wrapper.get('.guide-header').text()).toContain('등기부등본');
    expect(wrapper.findAll('[role="tab"]').length).toBeGreaterThan(0);
    expect(wrapper.get('[role="tab"]').attributes('aria-selected')).toBe('true');
  });

  it('전입신고 가이드 설정을 선택한다', () => {
    mocks.route.meta = { guideType: 'moveIn' };

    const wrapper = mountView();

    expect(wrapper.get('.guide-header').text()).toContain('전입신고');
  });
});
