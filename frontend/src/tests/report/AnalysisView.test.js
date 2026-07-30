import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import AnalysisView from '@/views/report/AnalysisView.vue';

const { getReport, replace, route } = vi.hoisted(() => ({
  getReport: vi.fn(),
  replace: vi.fn(),
  route: {
    meta: {},
    params: { analysisReportId: '12', scenario: undefined },
  },
}));

vi.mock('@/api/report', () => ({
  createReport: vi.fn(),
  getReport,
}));

vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ replace }),
}));

const report = {
  analysisReportId: 12,
  roadAddress: '서울시 마포구',
  detailAddress: '101호',
  deposit: 100_000_000,
  result: 'SAFE',
  favorite: false,
  checkResults: [],
  fraudTypes: [],
};

describe('분석 결과 화면', () => {
  beforeEach(() => {
    getReport.mockReset();
    replace.mockReset();
    route.meta = {};
    route.params = { analysisReportId: '12', scenario: undefined };
  });

  test('라우트의 보고서 ID로 상세 결과를 조회한다', async () => {
    getReport.mockResolvedValue(report);
    const wrapper = mount(AnalysisView);

    await flushPromises();

    expect(getReport).toHaveBeenCalledWith('12');
    expect(wrapper.text()).toContain('서울시 마포구 101호');
    expect(wrapper.text()).toContain('1억 0만원');
  });

  test('즐겨찾기는 비활성 상태로 시작하고 클릭 후 활성화된다', async () => {
    getReport.mockResolvedValue({ ...report, favorite: true });
    const wrapper = mount(AnalysisView);

    await flushPromises();

    const button = wrapper.get('.icon-button');
    expect(button.classes()).not.toContain('active');
    expect(button.attributes('aria-pressed')).toBe('false');

    await button.trigger('click');

    expect(button.classes()).toContain('active');
    expect(button.attributes('aria-pressed')).toBe('true');
  });

  test('상세 조회 실패 시 오류와 재시도 버튼을 표시한다', async () => {
    getReport.mockRejectedValue(new Error('network error'));
    const wrapper = mount(AnalysisView);

    await flushPromises();

    expect(wrapper.text()).toContain('분석 결과를 불러오지 못했습니다.');
    expect(wrapper.get('.report-feedback--error button').text()).toBe(
      '다시 시도',
    );
  });
});
