import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import AnalysisProgressView from './AnalysisProgressView.vue';

const mocks = vi.hoisted(() => ({
  replace: vi.fn(),
  runAnalysis: vi.fn(),
  useAnalysisFlow: vi.fn(),
  flow: {
    analysisStatus: { value: 'idle' },
    completedSteps: { value: 0 },
    currentMessage: { value: '분석 환경을 확인하고 있어요.' },
    errorMessage: { value: '' },
    progress: { value: 0 },
    totalSteps: 6,
  },
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: mocks.replace }),
}));

vi.mock('@/composables/report/useAnalysisFlow', () => ({
  useAnalysisFlow: mocks.useAnalysisFlow,
}));

describe('AnalysisProgressView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.flow.analysisStatus.value = 'idle';
    mocks.flow.completedSteps.value = 0;
    mocks.flow.currentMessage.value = '분석 환경을 확인하고 있어요.';
    mocks.flow.errorMessage.value = '';
    mocks.flow.progress.value = 0;
    mocks.useAnalysisFlow.mockReturnValue({
      ...mocks.flow,
      runAnalysis: mocks.runAnalysis,
    });
    window.history.replaceState({}, '');
  });

  it('mount 시 분석을 시작하고 성공 결과를 replace로 전달한다', async () => {
    const analysisRequest = {
      addressId: 'address-id',
      detailAddress: '202동 303호',
      deposit: 250_000_000,
    };
    const analysisResult = { analysisReportId: 41, roadAddress: 'demo' };
    const specialTermsResult = { specialTerms: [{ title: '특약' }] };
    window.history.replaceState({ analysisRequest }, '');
    mocks.runAnalysis.mockResolvedValue({
      analysisReportId: 41,
      analysisResult,
      specialTermsResult,
    });

    mount(AnalysisProgressView);
    await flushPromises();

    expect(mocks.useAnalysisFlow).toHaveBeenCalledWith({ analysisRequest });
    expect(mocks.runAnalysis).toHaveBeenCalledOnce();
    expect(mocks.replace).toHaveBeenCalledWith({
      name: 'analysis',
      params: { analysisReportId: 41 },
      state: { analysisResult, specialTermsResult },
    });
  });

  it('실패하면 같은 화면에 오류와 마지막 진행 단계를 유지한다', async () => {
    mocks.flow.analysisStatus.value = 'failed';
    mocks.flow.completedSteps.value = 4;
    mocks.flow.errorMessage.value = '분석을 완료하지 못했습니다.';
    mocks.flow.progress.value = (4 / 6) * 100;
    mocks.runAnalysis.mockResolvedValue(null);

    const wrapper = mount(AnalysisProgressView);
    await flushPromises();

    expect(mocks.replace).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('분석을 완료하지 못했어요.');
    expect(wrapper.text()).toContain('4 / 6');
    expect(wrapper.get('[role="progressbar"]').attributes('aria-valuenow')).toBe(
      '4',
    );
  });

  it('demo fallback 결과를 report/analysis/16 route로 전달한다', async () => {
    mocks.runAnalysis.mockResolvedValue({
      analysisReportId: 16,
      analysisResult: null,
      specialTermsResult: null,
    });

    mount(AnalysisProgressView);
    await flushPromises();

    expect(mocks.replace).toHaveBeenCalledWith({
      name: 'analysis',
      params: { analysisReportId: 16 },
      state: {
        analysisResult: null,
        specialTermsResult: null,
      },
    });
  });

  it('분석 중 화면을 떠나면 늦게 도착한 결과로 이동하지 않는다', async () => {
    let resolveAnalysis;
    mocks.runAnalysis.mockReturnValue(
      new Promise((resolve) => {
        resolveAnalysis = resolve;
      }),
    );
    const wrapper = mount(AnalysisProgressView);

    wrapper.unmount();
    resolveAnalysis({
      analysisReportId: 41,
      analysisResult: { analysisReportId: 41 },
      specialTermsResult: { specialTerms: [] },
    });
    await flushPromises();

    expect(mocks.replace).not.toHaveBeenCalled();
  });
});
