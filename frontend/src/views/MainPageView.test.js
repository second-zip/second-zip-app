import { flushPromises, mount } from '@vue/test-utils';
import { reactive } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import MainDataTabs from '@/components/main/MainDataTabs.vue';
import { logger } from '@/utils/logger';
import MainPageView from './MainPageView.vue';

const mocks = vi.hoisted(() => ({
  authStore: null,
  push: vi.fn(),
}));

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => mocks.authStore,
}));
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mocks.push }),
}));
vi.mock('@/utils/logger', () => ({
  logger: { error: vi.fn() },
}));

const SecretaryGuideStub = {
  name: 'SecretaryGuide',
  props: {
    text: String,
    characterType: String,
    changeBtn: Boolean,
  },
  emits: ['change'],
  template:
    '<button class="secretary-stub" @click="$emit(\'change\')">{{ characterType }}|{{ text }}</button>',
};
const ReportButtonStub = {
  name: 'ReportButton',
  emits: ['click'],
  template: '<button class="report-stub" @click="$emit(\'click\')">분석하기</button>',
};
const RiskMapCardStub = {
  name: 'RiskMapCard',
  template: '<section><slot /></section>',
};
const KoreaRegionMapStub = {
  name: 'KoreaRegionMap',
  props: ['dataType'],
  template: '<div class="korea-map-stub">{{ dataType }}</div>',
};

const mountView = () =>
  mount(MainPageView, {
    global: {
      stubs: {
        MainHero: true,
        RiskMapCard: RiskMapCardStub,
        KoreaRegionMap: KoreaRegionMapStub,
        ReportButton: ReportButtonStub,
        SecretaryGuide: SecretaryGuideStub,
      },
    },
  });

describe('MainPageView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authStore = reactive({
      isAuthenticated: false,
      myPage: null,
      fetchMyPage: vi.fn().mockResolvedValue(null),
    });
    mocks.push.mockResolvedValue(undefined);
  });

  it('비로그인 상태에서는 CAT 캐릭터와 피해주택 기본 문구를 사용한다', () => {
    const wrapper = mountView();
    const secretary = wrapper.getComponent(SecretaryGuideStub);

    expect(secretary.props()).toMatchObject({
      characterType: 'CAT',
      text: '피해 사례를 미리 확인하면\n더 안전한 계약이 가능하다냐-옹!',
      changeBtn: true,
    });
    expect(mocks.authStore.fetchMyPage).not.toHaveBeenCalled();
  });

  it.each([
    ['CAT', '피해 사례를 미리 확인하면\n더 안전한 계약이 가능하다냐-옹!'],
    ['MAN', '피해 사례를 미리 확인하면\n더 안전하게 계약할 수 있어!'],
    ['WOMAN', '더욱 안전한 계약을 위해\n피해 사례를 확인해 보시길 바랍니다!'],
  ])('%s 로그인 사용자의 피해주택 문구를 표시한다', (characterType, text) => {
    mocks.authStore.isAuthenticated = true;
    mocks.authStore.myPage = { characterType };
    const wrapper = mountView();

    expect(wrapper.getComponent(SecretaryGuideStub).props()).toMatchObject({
      characterType,
      text,
    });
  });

  it.each([
    ['CAT', '가격 변동률이 큰 지역은\n사기 위험도 높아질 수 있다냥…'],
    ['MAN', '가격 변동률이 큰 지역은\n사기 위험도 높을 수 있으니 조심해!'],
    ['WOMAN', '가격 변동률이 큰 지역은\n사기 위험도 높을 수 있으니 조심하세요…'],
  ])('%s 사용자가 가격지수 탭을 선택하면 전용 문구로 바꾼다', async (characterType, text) => {
    mocks.authStore.isAuthenticated = true;
    mocks.authStore.myPage = { characterType };
    const wrapper = mountView();
    const tabs = wrapper.getComponent(MainDataTabs);

    await tabs.vm.$emit('update:modelValue', 'price-index');

    expect(wrapper.getComponent(KoreaRegionMapStub).props('dataType')).toBe(
      'price-index',
    );
    expect(wrapper.getComponent(SecretaryGuideStub).props('text')).toBe(text);
  });

  it('로그인 토큰만 있으면 회원정보를 조회해 캐릭터를 복원한다', async () => {
    mocks.authStore.isAuthenticated = true;
    let resolveMyPage;
    mocks.authStore.fetchMyPage.mockImplementation(() =>
      new Promise((resolve) => {
        resolveMyPage = () => {
          mocks.authStore.myPage = { characterType: 'WOMAN' };
          resolve();
        };
      }),
    );
    const wrapper = mountView();

    expect(wrapper.findComponent(SecretaryGuideStub).exists()).toBe(false);

    resolveMyPage();
    await flushPromises();

    expect(mocks.authStore.fetchMyPage).toHaveBeenCalledOnce();
    expect(wrapper.getComponent(SecretaryGuideStub).props('characterType')).toBe(
      'WOMAN',
    );
  });

  it('회원정보 조회 실패 시 CAT fallback을 유지한다', async () => {
    const error = new Error('failure');
    mocks.authStore.isAuthenticated = true;
    mocks.authStore.fetchMyPage.mockRejectedValue(error);
    const wrapper = mountView();
    await flushPromises();

    expect(wrapper.getComponent(SecretaryGuideStub).props('characterType')).toBe(
      'CAT',
    );
    expect(logger.error).toHaveBeenCalledWith('main.fetch-user', error);
  });

  it('분석 및 캐릭터 변경 버튼을 요청 경로로 이동시킨다', async () => {
    const wrapper = mountView();

    await wrapper.get('.report-stub').trigger('click');
    await wrapper.get('.secretary-stub').trigger('click');

    expect(mocks.push).toHaveBeenNthCalledWith(1, '/report');
    expect(mocks.push).toHaveBeenNthCalledWith(2, '/mypage/character');
  });
});
