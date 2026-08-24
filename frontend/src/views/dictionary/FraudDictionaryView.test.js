import { mount, RouterLinkStub } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import FraudDictionaryView from './FraudDictionaryView.vue';

const mocks = vi.hoisted(() => ({
  authStore: {
    isAuthenticated: false,
    myPage: null,
    fetchMyPage: vi.fn(),
  },
  push: vi.fn(),
}));

vi.mock('@/stores/auth', () => ({ useAuthStore: () => mocks.authStore }));
vi.mock('vue-router', async (importOriginal) => ({
  ...(await importOriginal()),
  useRouter: () => ({ push: mocks.push }),
}));

describe('FraudDictionaryView', () => {
  beforeEach(() => vi.clearAllMocks());

  it('사기 유형을 표시하고 선택한 영상으로 이동한다', async () => {
    const wrapper = mount(FraudDictionaryView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    });
    const firstCard = wrapper.get('.fraud-card');

    expect(wrapper.findAll('.fraud-card')).toHaveLength(3);
    expect(firstCard.text()).toContain('무자본');

    await firstCard.get('button').trigger('click');

    expect(mocks.push).toHaveBeenCalledWith({
      name: 'dictionary-fraud-video',
      params: { typeId: 'gap-investment' },
    });
  });
});
