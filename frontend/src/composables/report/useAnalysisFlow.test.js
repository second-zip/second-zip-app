import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  DEMO_FALLBACK_REPORT_ID,
  useAnalysisFlow,
} from './useAnalysisFlow';

const apiMocks = vi.hoisted(() => ({
  completeAnalysis: vi.fn(),
  continueAnalysisAuth: vi.fn(),
  createAnalysisRequest: vi.fn(),
  createSpecialTerms: vi.fn(),
  getAnalysisRequest: vi.fn(),
  getExternalReadiness: vi.fn(),
  retryAnalysis: vi.fn(),
  startAnalysisAuth: vi.fn(),
}));

vi.mock('@/api/analysisReport', () => apiMocks);

const setDemoAuthEnv = () => {
  vi.stubEnv('VITE_ANALYSIS_TEST_BIRTH_DATE', 'TEST_BIRTH_DATE');
  vi.stubEnv('VITE_ANALYSIS_TEST_PHONE_NO', 'TEST_PHONE_NO');
  vi.stubEnv('VITE_ANALYSIS_TEST_USER_NAME', 'TEST_USER_NAME');
};

const TEST_ANALYSIS_REQUEST = Object.freeze({
  roadAddress: '서울특별시 강남구 테헤란로 1',
  detailAddress: '202동 303호',
  deposit: 250_000_000,
});

const createAnalysisFlow = () =>
  useAnalysisFlow({
    analysisRequest: TEST_ANALYSIS_REQUEST,
    authPollIntervalMs: 0,
    demoProgressIntervalMs: 0,
  });

describe('useAnalysisFlow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setDemoAuthEnv();

    apiMocks.getExternalReadiness.mockResolvedValue({ ready: true });
    apiMocks.getAnalysisRequest.mockResolvedValue({
      requestId: 'request-from-response',
      status: 'AUTH_PENDING',
      nextAction: 'SIMPLE_AUTH',
    });
    apiMocks.createAnalysisRequest.mockResolvedValue({
      requestId: 'request-from-response',
    });
    apiMocks.startAnalysisAuth.mockResolvedValue({
      requestId: 'request-from-response',
      status: 'AUTH_PENDING',
      nextAction: 'SIMPLE_AUTH',
    });
    apiMocks.continueAnalysisAuth.mockResolvedValue({
      requestId: 'request-from-response',
      status: 'PROCESSING',
    });
    apiMocks.completeAnalysis.mockResolvedValue({
      analysisReportId: 41,
      specialTerms: [],
    });
    apiMocks.retryAnalysis.mockResolvedValue({
      analysisReportId: 41,
      specialTerms: [],
    });
    apiMocks.createSpecialTerms.mockResolvedValue({
      specialTerms: [{ sequence: 1, title: '특약', content: '내용' }],
    });
  });

  it('6단계를 순서대로 한 번만 실행하고 응답 ID를 다음 요청에 사용한다', async () => {
    const order = [];

    Object.entries(apiMocks).forEach(([name, request]) => {
      const implementation = request.getMockImplementation();
      request.mockImplementation(async (...args) => {
        order.push(name);
        return implementation(...args);
      });
    });

    const flow = createAnalysisFlow();
    const firstRun = flow.runAnalysis();
    const duplicateRun = flow.runAnalysis();

    expect(duplicateRun).toBe(firstRun);
    await firstRun;

    expect(order).toEqual([
      'getExternalReadiness',
      'createAnalysisRequest',
      'startAnalysisAuth',
      'continueAnalysisAuth',
      'completeAnalysis',
      'createSpecialTerms',
    ]);
    expect(apiMocks.createAnalysisRequest).toHaveBeenCalledWith(
      TEST_ANALYSIS_REQUEST,
    );
    expect(apiMocks.startAnalysisAuth).toHaveBeenCalledWith(
      'request-from-response',
      {
        birthDate: 'TEST_BIRTH_DATE',
        consent: true,
        phoneNo: 'TEST_PHONE_NO',
        provider: 'KAKAO',
        telecom: 'SKT',
        userName: 'TEST_USER_NAME',
      },
    );
    expect(apiMocks.continueAnalysisAuth).toHaveBeenCalledWith(
      'request-from-response',
      {
        authentication: expect.objectContaining({
          userName: 'TEST_USER_NAME',
        }),
      },
    );
    expect(apiMocks.createSpecialTerms).toHaveBeenCalledWith(41);
    expect(flow.completedSteps.value).toBe(6);
    expect(flow.progress.value).toBe(100);
    expect(flow.analysisStatus.value).toBe('success');
  });

  it('complete 실패 시 retry 없이 demo 리포트로 완료 처리한다', async () => {
    apiMocks.completeAnalysis.mockRejectedValue(new Error('complete failed'));

    const flow = createAnalysisFlow();
    const result = await flow.runAnalysis();

    expect(apiMocks.retryAnalysis).not.toHaveBeenCalled();
    expect(apiMocks.createSpecialTerms).not.toHaveBeenCalled();
    expect(result).toEqual({
      analysisReportId: DEMO_FALLBACK_REPORT_ID,
      analysisResult: null,
      specialTermsResult: null,
    });
    expect(flow.analysisReportId.value).toBe(DEMO_FALLBACK_REPORT_ID);
    expect(flow.completedSteps.value).toBe(6);
    expect(flow.analysisStatus.value).toBe('success');
  });

  it('서로 다른 composable 인스턴스에서도 동시 실행을 중복하지 않는다', async () => {
    let resolveReadiness;
    apiMocks.getExternalReadiness.mockReturnValue(
      new Promise((resolve) => {
        resolveReadiness = resolve;
      }),
    );

    const firstFlow = createAnalysisFlow();
    const secondFlow = createAnalysisFlow();
    const firstRun = firstFlow.runAnalysis();
    const secondRun = secondFlow.runAnalysis();

    expect(secondRun).toBe(firstRun);
    expect(apiMocks.getExternalReadiness).toHaveBeenCalledOnce();

    resolveReadiness({ ready: true });
    await Promise.all([firstRun, secondRun]);

    expect(apiMocks.createAnalysisRequest).toHaveBeenCalledOnce();
    expect(apiMocks.createSpecialTerms).toHaveBeenCalledOnce();
  });

  it('special-terms 실패도 demo 리포트로 완료 처리한다', async () => {
    apiMocks.createSpecialTerms.mockRejectedValue(new Error('terms failed'));

    const flow = createAnalysisFlow();
    const result = await flow.runAnalysis();

    expect(apiMocks.retryAnalysis).not.toHaveBeenCalled();
    expect(result.analysisReportId).toBe(DEMO_FALLBACK_REPORT_ID);
    expect(flow.completedSteps.value).toBe(6);
    expect(flow.failedStep.value).toBe(6);
    expect(flow.analysisStatus.value).toBe('success');
  });

  it('readiness가 준비되지 않았으면 다음 API를 호출하지 않는다', async () => {
    apiMocks.getExternalReadiness.mockResolvedValue({ ready: false });

    const flow = createAnalysisFlow();
    const result = await flow.runAnalysis();

    expect(apiMocks.createAnalysisRequest).not.toHaveBeenCalled();
    expect(apiMocks.retryAnalysis).not.toHaveBeenCalled();
    expect(result.analysisReportId).toBe(DEMO_FALLBACK_REPORT_ID);
    expect(flow.completedSteps.value).toBe(6);
    expect(flow.failedStep.value).toBe(1);
    expect(flow.analysisStatus.value).toBe('success');
  });

  it('demo fallback progress를 남은 단계마다 순차적으로 증가시킨다', async () => {
    vi.useFakeTimers();
    apiMocks.getExternalReadiness.mockResolvedValue({ ready: false });
    const flow = useAnalysisFlow({
      authPollIntervalMs: 0,
      demoProgressIntervalMs: 100,
    });

    try {
      const run = flow.runAnalysis();

      await vi.advanceTimersByTimeAsync(100);
      expect(flow.completedSteps.value).toBe(1);

      await vi.advanceTimersByTimeAsync(200);
      expect(flow.completedSteps.value).toBe(3);

      await vi.advanceTimersByTimeAsync(300);
      await run;
      expect(flow.completedSteps.value).toBe(6);
    } finally {
      vi.useRealTimers();
    }
  });

  it('인증 환경변수가 없으면 auth/start를 호출하지 않는다', async () => {
    vi.stubEnv('VITE_ANALYSIS_TEST_BIRTH_DATE', '');
    vi.stubEnv('VITE_ANALYSIS_TEST_PHONE_NO', '');
    vi.stubEnv('VITE_ANALYSIS_TEST_USER_NAME', '');

    const flow = createAnalysisFlow();
    await flow.runAnalysis();

    expect(apiMocks.startAnalysisAuth).not.toHaveBeenCalled();
    expect(apiMocks.retryAnalysis).not.toHaveBeenCalled();
    expect(flow.completedSteps.value).toBe(6);
    expect(flow.failedStep.value).toBe(3);
    expect(flow.analysisReportId.value).toBe(DEMO_FALLBACK_REPORT_ID);
    expect(flow.analysisStatus.value).toBe('success');
    expect(flow.errorMessage.value).toBe('분석 인증 설정을 확인해주세요.');
  });

  it('주소·동·호 선택과 추가 문서 인증을 순서대로 완료한다', async () => {
    apiMocks.startAnalysisAuth
      .mockResolvedValueOnce({
        status: 'AUTH_PENDING',
        nextAction: 'SIMPLE_AUTH',
      })
      .mockResolvedValueOnce({ status: 'PROCESSING', nextAction: 'NONE' });
    apiMocks.continueAnalysisAuth
      .mockResolvedValueOnce({
        status: 'SELECTION_REQUIRED',
        nextAction: 'ADDRESS_SELECTION',
        selectionOptions: [
          {
            value: 'selected-address',
            label: '서울특별시 강남구 테헤란로 1',
          },
        ],
      })
      .mockResolvedValueOnce({
        status: 'SELECTION_REQUIRED',
        nextAction: 'DONG_SELECTION',
        selectionOptions: [{ value: '202', label: '202동' }],
      })
      .mockResolvedValueOnce({
        status: 'SELECTION_REQUIRED',
        nextAction: 'HO_SELECTION',
        selectionOptions: [{ value: '303', label: '303호' }],
      })
      .mockResolvedValueOnce({ status: 'AUTH_REQUIRED', nextAction: 'NONE' });

    const flow = createAnalysisFlow();
    await flow.runAnalysis();

    expect(apiMocks.startAnalysisAuth).toHaveBeenCalledTimes(2);
    expect(apiMocks.continueAnalysisAuth).toHaveBeenCalledTimes(4);
    expect(apiMocks.continueAnalysisAuth).toHaveBeenNthCalledWith(
      2,
      'request-from-response',
      expect.objectContaining({ selectionValue: 'selected-address' }),
    );
    expect(apiMocks.continueAnalysisAuth).toHaveBeenNthCalledWith(
      3,
      'request-from-response',
      expect.objectContaining({ selectionValue: '202' }),
    );
    expect(apiMocks.continueAnalysisAuth).toHaveBeenNthCalledWith(
      4,
      'request-from-response',
      expect.objectContaining({ selectionValue: '303' }),
    );
    expect(flow.completedSteps.value).toBe(6);
    expect(flow.analysisStatus.value).toBe('success');
  });

  it('카카오 승인이 대기 중이면 PROCESSING이 될 때까지 제한적으로 확인한다', async () => {
    apiMocks.continueAnalysisAuth
      .mockResolvedValueOnce({
        status: 'AUTH_PENDING',
        nextAction: 'SIMPLE_AUTH',
      })
      .mockResolvedValueOnce({
        status: 'AUTH_PENDING',
        nextAction: 'SIMPLE_AUTH',
      })
      .mockResolvedValueOnce({ status: 'PROCESSING', nextAction: 'NONE' });

    const flow = createAnalysisFlow();
    await flow.runAnalysis();

    expect(apiMocks.continueAnalysisAuth).toHaveBeenCalledTimes(3);
    expect(flow.completedSteps.value).toBe(6);
    expect(flow.analysisStatus.value).toBe('success');
  });

  it('continue의 일시적 실패 후 서버 상태를 조회해 흐름을 복구한다', async () => {
    apiMocks.continueAnalysisAuth.mockRejectedValueOnce({
      response: { status: 502 },
    });
    apiMocks.getAnalysisRequest.mockResolvedValueOnce({
      requestId: 'request-from-response',
      status: 'PROCESSING',
      nextAction: 'NONE',
    });

    const flow = createAnalysisFlow();
    await flow.runAnalysis();

    expect(apiMocks.getAnalysisRequest).toHaveBeenCalledWith(
      'request-from-response',
    );
    expect(apiMocks.completeAnalysis).toHaveBeenCalledOnce();
    expect(flow.completedSteps.value).toBe(6);
    expect(flow.analysisStatus.value).toBe('success');
  });

  it('CAPTCHA가 필요해도 임의 값을 보내지 않고 demo 리포트로 이동한다', async () => {
    apiMocks.continueAnalysisAuth.mockResolvedValueOnce({
      status: 'SELECTION_REQUIRED',
      nextAction: 'CAPTCHA',
      captchaImage: 'captcha-image',
    });

    const flow = createAnalysisFlow();
    const result = await flow.runAnalysis();

    expect(apiMocks.continueAnalysisAuth).toHaveBeenCalledOnce();
    expect(apiMocks.completeAnalysis).not.toHaveBeenCalled();
    expect(apiMocks.retryAnalysis).not.toHaveBeenCalled();
    expect(result.analysisReportId).toBe(DEMO_FALLBACK_REPORT_ID);
    expect(flow.completedSteps.value).toBe(6);
    expect(flow.failedStep.value).toBe(4);
    expect(flow.analysisStatus.value).toBe('success');
    expect(flow.errorMessage.value).toContain('보안문자 입력');
  });
});
