import { beforeEach, describe, expect, it, vi } from 'vitest';

import api from './instance';
import {
  completeAnalysis,
  continueAnalysisAuth,
  createAnalysisRequest,
  createSpecialTerms,
  getAnalysisRequest,
  getExternalReadiness,
  retryAnalysis,
  startAnalysisAuth,
} from './analysisReport';

vi.mock('./instance', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}));

describe('analysis report API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.get.mockResolvedValue({ data: { ready: true } });
    api.post.mockResolvedValue({ data: { ok: true } });
  });

  it('외부 API 준비 상태를 GET으로 조회한다', async () => {
    await expect(getExternalReadiness()).resolves.toEqual({ ready: true });
    expect(api.get).toHaveBeenCalledWith(
      '/analysis-reports/external-readiness',
    );
  });

  it('requestId로 현재 분석 요청 상태를 GET 조회한다', async () => {
    await expect(getAnalysisRequest('request-id')).resolves.toEqual({
      ready: true,
    });
    expect(api.get).toHaveBeenCalledWith(
      '/analysis-reports/requests/request-id',
    );
  });

  it('분석 요청과 인증 요청 body를 POST로 전달한다', async () => {
    const requestPayload = { roadAddress: 'demo-address' };
    const authPayload = { userName: 'TEST_USER_NAME' };
    const continuePayload = { authentication: authPayload };

    await createAnalysisRequest(requestPayload);
    await startAnalysisAuth('request-id', authPayload);
    await continueAnalysisAuth('request-id', continuePayload);

    expect(api.post).toHaveBeenNthCalledWith(
      1,
      '/analysis-reports/requests',
      requestPayload,
    );
    expect(api.post).toHaveBeenNthCalledWith(
      2,
      '/analysis-reports/requests/request-id/auth/start',
      authPayload,
      { timeout: 300_000 },
    );
    expect(api.post).toHaveBeenNthCalledWith(
      3,
      '/analysis-reports/requests/request-id/auth/continue',
      continuePayload,
      { timeout: 300_000 },
    );
  });

  it('complete, retry, special-terms를 body 없이 POST한다', async () => {
    await completeAnalysis('request-id');
    await retryAnalysis('request-id');
    await createSpecialTerms(41);

    expect(api.post).toHaveBeenNthCalledWith(
      1,
      '/analysis-reports/requests/request-id/complete',
      undefined,
      { timeout: 300_000 },
    );
    expect(api.post).toHaveBeenNthCalledWith(
      2,
      '/analysis-reports/requests/request-id/retry',
      undefined,
      { timeout: 300_000 },
    );
    expect(api.post).toHaveBeenNthCalledWith(
      3,
      '/analysis-reports/41/special-terms',
      undefined,
      { timeout: 300_000 },
    );
  });
});
