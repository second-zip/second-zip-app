import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import FavoriteIcon from '@/assets/icons/report/favorite-blue-18.svg';
import FavoriteLineIcon from '@/assets/icons/report/favorite-line-18.svg';
import ReportIcon from '@/assets/icons/report/report-blue-18.svg';
import AnalysisView from '@/views/report/AnalysisView.vue';
import { logger } from '@/utils/logger';

const mocks = vi.hoisted(() => ({
  addReportFavorite: vi.fn(),
  authStore: {
    characterType: 'CAT',
    fetchMyPage: vi.fn(),
    isAuthenticated: false,
    myPage: null,
  },
  clipboardWrite: vi.fn(),
  createChecklist: vi.fn(),
  deleteReportFavorite: vi.fn(),
  getChecklists: vi.fn(),
  getReport: vi.fn(),
  getSharedReport: vi.fn(),
  push: vi.fn(),
  replace: vi.fn(),
  resolve: vi.fn(({ params }) => ({
    href: `/report/shared/${params.shareToken}`,
  })),
  route: {
    meta: {},
    params: {
      analysisReportId: '12',
      scenario: undefined,
      shareToken: undefined,
    },
  },
  shareReport: vi.fn(),
}));

vi.mock('@/api/report', () => ({
  addReportFavorite: mocks.addReportFavorite,
  deleteReportFavorite: mocks.deleteReportFavorite,
  getReport: mocks.getReport,
  getSharedReport: mocks.getSharedReport,
  shareReport: mocks.shareReport,
}));
vi.mock('@/api/checklist', () => ({
  createChecklist: mocks.createChecklist,
  getChecklists: mocks.getChecklists,
}));

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => ({
    push: mocks.push,
    replace: mocks.replace,
    resolve: mocks.resolve,
  }),
}));
vi.mock('@/utils/logger', () => ({
  logger: { error: vi.fn() },
}));
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => mocks.authStore,
}));

const report = {
  analysisReportId: 12,
  roadAddress: '서울시 마포구',
  detailAddress: '101호',
  deposit: 100_000_000,
  result: 'SAFE',
  ratio: 0.5556,
  favorite: false,
  checkResults: [],
  fraudTypes: [],
};

describe('분석 결과 화면', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authStore.isAuthenticated = false;
    mocks.authStore.myPage = null;
    mocks.authStore.fetchMyPage.mockResolvedValue({ characterType: 'CAT' });
    mocks.getReport.mockResolvedValue(report);
    mocks.getChecklists.mockResolvedValue([]);
    mocks.shareReport.mockResolvedValue({ shareToken: 'share-token' });
    mocks.route.meta = {};
    mocks.route.params = {
      analysisReportId: '12',
      scenario: undefined,
      shareToken: undefined,
    };
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: mocks.clipboardWrite },
    });
    window.history.replaceState({}, '');
  });

  test('라우트의 보고서 ID로 상세 결과를 조회한다', async () => {
    const wrapper = mount(AnalysisView);

    await flushPromises();

    expect(mocks.getReport).toHaveBeenCalledWith('12');
    expect(wrapper.text()).toContain('서울시 마포구 101호');
    expect(wrapper.text()).toContain('1억 0만원');
    expect(wrapper.get('.ratio-field strong').text()).toBe('55.56%');
    expect(wrapper.get('.address-icon').attributes('src')).toBe(ReportIcon);
  });

  test('위험도 요약은 세부 항목 집계가 아닌 상세 응답의 result를 표시한다', async () => {
    mocks.getReport.mockResolvedValue({
      ...report,
      result: 'DANGER',
      checkResults: [],
      fraudTypes: [],
    });

    const wrapper = mount(AnalysisView);
    await flushPromises();

    expect(wrapper.get('.risk-summary').classes()).toContain(
      'risk-summary--danger',
    );
  });

  test('로그인 프로필 조회 실패를 기록하고 리포트는 계속 표시한다', async () => {
    const error = new Error('profile failed');
    mocks.authStore.isAuthenticated = true;
    mocks.authStore.fetchMyPage.mockRejectedValue(error);

    const wrapper = mount(AnalysisView);
    await flushPromises();

    expect(logger.error).toHaveBeenCalledWith('analysis.fetch-user', error);
    expect(wrapper.text()).toContain('서울시 마포구 101호');
  });

  test('미리보기 라우트는 API 요청 없이 시나리오 데이터를 표시한다', async () => {
    mocks.route.meta = { analysisPreview: true };
    mocks.route.params = {
      analysisReportId: undefined,
      scenario: 'b',
      shareToken: undefined,
    };

    const wrapper = mount(AnalysisView);
    await flushPromises();

    expect(mocks.getReport).not.toHaveBeenCalled();
    expect(wrapper.get('.risk-summary').classes()).toContain(
      'risk-summary--danger',
    );
    expect(wrapper.get('.ratio-field strong').text()).toBe('85%');
  });

  test('상세 응답의 즐겨찾기 상태를 표시하고 해제 API를 호출한다', async () => {
    mocks.getReport.mockResolvedValue({ ...report, favorite: true });
    const wrapper = mount(AnalysisView);

    await flushPromises();

    const button = wrapper.get('.favorite-button');
    expect(button.classes()).toContain('active');
    expect(button.attributes('aria-pressed')).toBe('true');
    expect(button.get('img').attributes('src')).toBe(FavoriteIcon);

    await button.trigger('click');
    await flushPromises();

    expect(mocks.deleteReportFavorite).toHaveBeenCalledWith(12);
    expect(button.classes()).not.toContain('active');
    expect(button.attributes('aria-pressed')).toBe('false');
    expect(button.get('img').attributes('src')).toBe(FavoriteLineIcon);
  });

  test('즐겨찾기가 없는 리포트는 추가 API를 호출한다', async () => {
    const wrapper = mount(AnalysisView);
    await flushPromises();

    await wrapper.get('.favorite-button').trigger('click');
    await flushPromises();

    expect(mocks.addReportFavorite).toHaveBeenCalledWith(12);
    expect(
      wrapper.get('.favorite-button').attributes('aria-pressed'),
    ).toBe('true');
  });

  test('공유 API의 토큰으로 열람 링크를 복사한다', async () => {
    const wrapper = mount(AnalysisView);
    await flushPromises();

    await wrapper.get('.share-button').trigger('click');
    await flushPromises();

    expect(mocks.shareReport).toHaveBeenCalledWith(12);
    expect(mocks.resolve).toHaveBeenCalledWith({
      name: 'analysis-shared',
      params: { shareToken: 'share-token' },
    });
    expect(mocks.clipboardWrite).toHaveBeenCalledWith(
      `${window.location.origin}/report/shared/share-token`,
    );
    expect(wrapper.text()).toContain('링크를 복사했습니다.');
  });

  test('클립보드 API 실패 시 textarea 방식으로 링크를 복사한다', async () => {
    mocks.clipboardWrite.mockRejectedValue(new Error('denied'));
    Object.defineProperty(document, 'execCommand', {
      configurable: true,
      value: vi.fn(() => true),
    });
    const wrapper = mount(AnalysisView);
    await flushPromises();

    await wrapper.get('.share-button').trigger('click');
    await flushPromises();

    expect(document.execCommand).toHaveBeenCalledWith('copy');
    expect(document.querySelector('textarea')).toBeNull();
  });

  test('복사 알림은 지정 시간이 지나면 자동으로 숨긴다', async () => {
    const wrapper = mount(AnalysisView);
    await flushPromises();
    const timeoutSpy = vi.spyOn(window, 'setTimeout').mockImplementation((callback) => {
      callback();
      return 1;
    });

    await wrapper.get('.share-button').trigger('click');
    await flushPromises();

    expect(timeoutSpy).toHaveBeenCalledWith(expect.any(Function), 2000);
    expect(wrapper.text()).not.toContain('링크를 복사했습니다.');
    timeoutSpy.mockRestore();
  });

  test('즐겨찾기 실패를 화면에 표시한다', async () => {
    mocks.addReportFavorite.mockRejectedValue({
      response: { data: { message: '즐겨찾기 실패' } },
    });
    const wrapper = mount(AnalysisView);
    await flushPromises();

    await wrapper.get('.favorite-button').trigger('click');
    await flushPromises();

    expect(wrapper.get('.result-actions__error').text()).toBe(
      '즐겨찾기 실패',
    );
    expect(logger.error).toHaveBeenCalledWith(
      'analysis.toggle-favorite',
      expect.anything(),
      { analysisReportId: 12 },
    );
  });

  test('공유 토큰이 없으면 오류를 표시한다', async () => {
    mocks.shareReport.mockResolvedValue({});
    const wrapper = mount(AnalysisView);
    await flushPromises();

    await wrapper.get('.share-button').trigger('click');
    await flushPromises();

    expect(wrapper.get('.result-actions__error').text()).not.toBe('');
    expect(logger.error).toHaveBeenCalledWith(
      'analysis.share-report',
      expect.any(Error),
      { analysisReportId: 12 },
    );
  });

  test('목록 보기는 리포트 목록으로 이동한다', async () => {
    const wrapper = mount(AnalysisView);
    await flushPromises();

    expect(
      wrapper
        .get('.analysis-content')
        .find('.result-actions')
        .exists(),
    ).toBe(true);
    expect(
      wrapper.findAll('.result-actions__button').map((button) => button.text()),
    ).toEqual(['목록 보기', '체크리스트 확인']);

    await wrapper.get('.result-actions__button--secondary').trigger('click');

    expect(mocks.push).toHaveBeenCalledWith({ name: 'report-list' });
  });

  test('기존 체크리스트가 있으면 상세 화면으로 이동한다', async () => {
    mocks.getChecklists.mockResolvedValue([
      {
        analysisReportId: 12,
        checklistCreated: true,
        reportChecklistId: 44,
      },
    ]);
    const wrapper = mount(AnalysisView);
    await flushPromises();

    await wrapper.get('.result-actions__button--primary').trigger('click');
    await flushPromises();

    expect(mocks.createChecklist).not.toHaveBeenCalled();
    expect(mocks.push).toHaveBeenCalledWith({
      name: 'checklist-detail',
      params: { reportChecklistId: 44 },
    });
  });

  test('체크리스트가 없으면 생성한 뒤 상세 화면으로 이동한다', async () => {
    mocks.createChecklist.mockResolvedValue({ reportChecklistId: 55 });
    const wrapper = mount(AnalysisView);
    await flushPromises();

    await wrapper.get('.result-actions__button--primary').trigger('click');
    await flushPromises();

    expect(mocks.createChecklist).toHaveBeenCalledWith(12);
    expect(mocks.push).toHaveBeenCalledWith({
      name: 'checklist-detail',
      params: { reportChecklistId: 55 },
    });
  });

  test('체크리스트 확인 실패를 화면에 표시한다', async () => {
    mocks.getChecklists.mockRejectedValue({
      response: { data: { message: '체크리스트 실패' } },
    });
    const wrapper = mount(AnalysisView);
    await flushPromises();

    await wrapper.get('.result-actions__button--primary').trigger('click');
    await flushPromises();

    expect(wrapper.get('.result-actions__error').text()).toBe(
      '체크리스트 실패',
    );
    expect(logger.error).toHaveBeenCalledWith(
      'analysis.open-checklist',
      expect.anything(),
      { analysisReportId: 12 },
    );
  });

  test('공유 라우트는 토큰으로 리포트를 조회하고 소유자 동작을 숨긴다', async () => {
    mocks.route.meta = { analysisShared: true };
    mocks.route.params = {
      analysisReportId: undefined,
      scenario: undefined,
      shareToken: 'share-token',
    };
    mocks.getSharedReport.mockResolvedValue(report);

    const wrapper = mount(AnalysisView);
    await flushPromises();

    expect(mocks.getSharedReport).toHaveBeenCalledWith('share-token');
    expect(wrapper.find('.header-actions').exists()).toBe(false);
    expect(wrapper.find('.result-actions').exists()).toBe(false);
  });

  test('상세 조회 실패 시 오류와 재시도 버튼을 표시한다', async () => {
    const error = new Error('network error');
    mocks.getReport.mockRejectedValue(error);
    const wrapper = mount(AnalysisView);

    await flushPromises();

    expect(wrapper.text()).toContain('분석 결과를 불러오지 못했습니다.');
    expect(wrapper.get('.report-feedback--error button').text()).toBe(
      '다시 시도',
    );
    expect(logger.error).toHaveBeenCalledWith(
      'analysis.load-report',
      error,
      { analysisReportId: '12' },
    );
  });

  test('분석 진행 화면에서 전달한 분석·특약 응답을 우선 사용한다', async () => {
    window.history.replaceState(
      {
        analysisResult: report,
        specialTermsResult: {
          specialTerms: [
            { sequence: 1, title: '전달된 특약', content: '특약 내용' },
          ],
        },
      },
      '',
    );

    const wrapper = mount(AnalysisView);
    await flushPromises();

    expect(mocks.getReport).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('서울시 마포구 101호');
    expect(wrapper.text()).toContain('전달된 특약');
  });
});
