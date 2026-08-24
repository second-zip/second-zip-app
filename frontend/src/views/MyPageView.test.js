import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MyPageView from './MyPageView.vue';

const mocks = vi.hoisted(() => ({
  getReports: vi.fn(),
  replace: vi.fn(),
  authStore: {
    fetchMyPage: vi.fn(),
    logout: vi.fn(),
    myPageLoading: false,
    loading: false,
    isAuthenticated: true,
    characterType: 'CAT',
    myPage: {
      email: 'user@example.com',
      nickname: '테스터',
      characterType: 'CAT',
    },
  },
}));

vi.mock('@/api/report', () => ({ getReports: mocks.getReports }));
vi.mock('@/stores/auth', () => ({ useAuthStore: () => mocks.authStore }));
vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: mocks.replace }),
}));

describe('MyPageView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authStore.isAuthenticated = true;
    mocks.authStore.logout.mockResolvedValue(undefined);
    mocks.authStore.fetchMyPage.mockResolvedValue(mocks.authStore.myPage);
    mocks.authStore.characterType = 'CAT';
    mocks.getReports.mockResolvedValue({
      totalCount: 4,
      reports: [
        { result: 'SAFE' },
        { result: 'SAFE' },
        { result: 'CAUTION' },
        { result: 'DANGER' },
      ],
    });
  });

  it('회원정보와 실제 리포트 활동 요약을 표시한다', async () => {
    const wrapper = mount(MyPageView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    });
    await flushPromises();
    expect(wrapper.text()).not.toContain('테스터 님');
    expect(wrapper.text()).toContain('ID');
    expect(wrapper.get('.profile-card__nickname').text()).toBe(
      mocks.authStore.myPage.nickname,
    );
    expect(wrapper.text()).toContain('user@example.com');
    expect(
      wrapper.findAll('.activity-card__item').map((item) => item.text()),
    ).toEqual(['4총 리포트', '2안전', '1주의', '1위험']);
  });

  it('각 관리 버튼을 이름 기반 하위 라우트에 연결한다', async () => {
    const wrapper = mount(MyPageView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    });
    await flushPromises();
    const links = wrapper.findAllComponents(RouterLinkStub);
    expect(links.map((link) => link.props('to'))).toEqual(
      expect.arrayContaining([
        { name: 'mypage-profile' },
        { name: 'mypage-secretary' },
        { name: 'mypage-withdraw' },
      ]),
    );
  });

  it('현재 Store에 적용된 AI 비서 이름을 활동 요약에 표시한다', async () => {
    mocks.authStore.characterType = 'WOMAN';
    const wrapper = mount(MyPageView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    });
    await flushPromises();

    expect(wrapper.text()).toContain('엘리스');
    expect(wrapper.text()).toContain('현재 AI 비서');
  });

  it('로그인 사용자를 로그아웃하고 로그인 화면으로 이동한다', async () => {
    const wrapper = mount(MyPageView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    });
    await flushPromises();

    await wrapper.get('.mypage__menu button').trigger('click');
    await flushPromises();

    expect(mocks.authStore.logout).toHaveBeenCalledOnce();
    expect(mocks.replace).toHaveBeenCalledWith({ name: 'login' });
  });

  it('비로그인 상태에서는 서버 로그아웃 없이 로그인으로 이동한다', async () => {
    mocks.authStore.isAuthenticated = false;
    const wrapper = mount(MyPageView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    });
    await flushPromises();

    await wrapper.get('.mypage__menu button').trigger('click');

    expect(mocks.authStore.logout).not.toHaveBeenCalled();
    expect(mocks.replace).toHaveBeenCalledWith({ name: 'login' });
  });

  it('로그아웃 실패 메시지를 표시한다', async () => {
    mocks.authStore.logout.mockRejectedValue({
      response: { data: { message: '로그아웃 실패' } },
    });
    const wrapper = mount(MyPageView, {
      global: { stubs: { RouterLink: RouterLinkStub } },
    });
    await flushPromises();

    await wrapper.get('.mypage__menu button').trigger('click');
    await flushPromises();

    expect(wrapper.get('[role="alert"]').text()).toBe('로그아웃 실패');
  });
});
