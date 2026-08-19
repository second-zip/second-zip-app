import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  completeAnalysis,
  createAnalysisRequest,
  createSpecialTerms,
  getExternalReadiness,
  startAnalysisAuth,
} from '@/api/analysisReport';
import { finishAnalysisAuthentication } from './analysisAuth';
import { executeAnalysisFlow } from './analysisFlow';

vi.mock('@/api/analysisReport', () => ({
  completeAnalysis: vi.fn(),
  createAnalysisRequest: vi.fn(),
  createSpecialTerms: vi.fn(),
  getExternalReadiness: vi.fn(),
  startAnalysisAuth: vi.fn(),
}));
vi.mock('./analysisAuth', () => ({
  finishAnalysisAuthentication: vi.fn(),
}));

const ANALYSIS_REQUEST = {
  addressId: 'address-id',
  detailAddress: '202동 303호',
  deposit: 250_000_000,
};

describe('analysis flow service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubEnv('VITE_ANALYSIS_TEST_BIRTH_DATE', 'TEST_BIRTH');
    vi.stubEnv('VITE_ANALYSIS_TEST_PHONE_NO', 'TEST_PHONE');
    vi.stubEnv('VITE_ANALYSIS_TEST_USER_NAME', 'TEST_USER');
    getExternalReadiness.mockResolvedValue({ ready: true });
    createAnalysisRequest.mockResolvedValue({
      requestId: 'request-id',
      roadAddress: '서울 강남구 테헤란로 1',
    });
    startAnalysisAuth.mockResolvedValue({ status: 'AUTH_PENDING' });
    finishAnalysisAuthentication.mockResolvedValue({ status: 'PROCESSING' });
    completeAnalysis.mockResolvedValue({ analysisReportId: 41 });
    createSpecialTerms.mockResolvedValue({ specialTerms: ['term'] });
  });

  afterEach(() => vi.unstubAllEnvs());

  it('6단계를 순차적으로 실행하고 ID와 응답을 반환한다', async () => {
    const events = [];

    await expect(
      executeAnalysisFlow({
        analysisRequest: {
          ...ANALYSIS_REQUEST,
          roadAddress: '서울 강남구 테헤란로 1',
        },
        authPollIntervalMs: 0,
        onStepStart: (step) => events.push(`start:${step}`),
        onStepComplete: (step) => events.push(`complete:${step}`),
      }),
    ).resolves.toEqual({
      requestId: 'request-id',
      analysisReportId: 41,
      analysisResult: { analysisReportId: 41 },
      specialTermsResult: { specialTerms: ['term'] },
    });

    expect(events).toEqual(
      [1, 2, 3, 4, 5, 6].flatMap((step) => [
        `start:${step}`,
        `complete:${step}`,
      ]),
    );
    expect(createAnalysisRequest).toHaveBeenCalledWith(ANALYSIS_REQUEST);
    expect(startAnalysisAuth).toHaveBeenCalledWith(
      'request-id',
      expect.objectContaining({ userName: 'TEST_USER' }),
    );
    expect(finishAnalysisAuthentication).toHaveBeenCalledWith(
      expect.objectContaining({
        requestId: 'request-id',
        analysisRequest: {
          ...ANALYSIS_REQUEST,
          roadAddress: '서울 강남구 테헤란로 1',
        },
      }),
    );
    expect(completeAnalysis).toHaveBeenCalledWith('request-id');
    expect(createSpecialTerms).toHaveBeenCalledWith(41);
  });

  it('readiness가 false면 1단계에서 중단한다', async () => {
    getExternalReadiness.mockResolvedValue({ ready: false });

    await expect(
      executeAnalysisFlow({
        analysisRequest: ANALYSIS_REQUEST,
        onStepStart: vi.fn(),
        onStepComplete: vi.fn(),
      }),
    ).rejects.toThrow('분석 환경이 준비되지 않았습니다.');
    expect(createAnalysisRequest).not.toHaveBeenCalled();
  });

  it('requestId나 analysisReportId가 없으면 다음 단계를 호출하지 않는다', async () => {
    createAnalysisRequest.mockResolvedValue({});

    await expect(
      executeAnalysisFlow({
        analysisRequest: ANALYSIS_REQUEST,
        onStepStart: vi.fn(),
        onStepComplete: vi.fn(),
      }),
    ).rejects.toThrow('분석 요청 정보를 확인하지 못했습니다.');
    expect(startAnalysisAuth).not.toHaveBeenCalled();

    createAnalysisRequest.mockResolvedValue({ requestId: 'request-id' });
    completeAnalysis.mockResolvedValue({});
    await expect(
      executeAnalysisFlow({
        analysisRequest: ANALYSIS_REQUEST,
        authPollIntervalMs: 0,
        onStepStart: vi.fn(),
        onStepComplete: vi.fn(),
      }),
    ).rejects.toThrow('분석 결과 정보를 확인하지 못했습니다.');
    expect(createSpecialTerms).not.toHaveBeenCalled();
  });
});
