import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  continueAnalysisAuth,
  getAnalysisRequest,
  startAnalysisAuth,
} from '@/api/analysisReport';
import { finishAnalysisAuthentication } from './analysisAuth';

vi.mock('@/api/analysisReport', () => ({
  continueAnalysisAuth: vi.fn(),
  getAnalysisRequest: vi.fn(),
  startAnalysisAuth: vi.fn(),
}));

const BASE_OPTIONS = {
  requestId: 'request-id',
  authPayload: { userName: 'TEST_USER' },
  analysisRequest: {
    roadAddress: '서울 강남구 테헤란로 1',
    detailAddress: '202동 303호',
  },
  pollIntervalMs: 0,
};

describe('analysis authentication service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('PROCESSING 상태는 추가 요청 없이 완료한다', async () => {
    const response = { status: 'PROCESSING' };

    await expect(
      finishAnalysisAuthentication({
        ...BASE_OPTIONS,
        initialResponse: response,
      }),
    ).resolves.toBe(response);
    expect(startAnalysisAuth).not.toHaveBeenCalled();
    expect(continueAnalysisAuth).not.toHaveBeenCalled();
  });

  it('AUTH_REQUIRED면 인증을 다시 시작한다', async () => {
    startAnalysisAuth.mockResolvedValue({ status: 'PROCESSING' });

    await finishAnalysisAuthentication({
      ...BASE_OPTIONS,
      initialResponse: { status: 'AUTH_REQUIRED' },
    });

    expect(startAnalysisAuth).toHaveBeenCalledWith(
      BASE_OPTIONS.requestId,
      BASE_OPTIONS.authPayload,
    );
  });

  it('SIMPLE_AUTH 대기 상태를 continue해 완료한다', async () => {
    continueAnalysisAuth.mockResolvedValue({ status: 'PROCESSING' });

    await finishAnalysisAuthentication({
      ...BASE_OPTIONS,
      initialResponse: {
        status: 'AUTH_PENDING',
        nextAction: 'SIMPLE_AUTH',
      },
    });

    expect(continueAnalysisAuth).toHaveBeenCalledWith('request-id', {
      authentication: BASE_OPTIONS.authPayload,
    });
  });

  it('continue의 일시적 오류 후 request 상태 조회로 복구한다', async () => {
    continueAnalysisAuth.mockRejectedValue({ response: { status: 502 } });
    getAnalysisRequest.mockResolvedValue({ status: 'PROCESSING' });

    await finishAnalysisAuthentication({
      ...BASE_OPTIONS,
      initialResponse: {
        status: 'AUTH_PENDING',
        nextAction: 'SIMPLE_AUTH',
      },
    });

    expect(getAnalysisRequest).toHaveBeenCalledWith('request-id');
  });

  it('복구할 수 없는 continue 오류는 즉시 전파한다', async () => {
    const error = { response: { status: 400 } };
    continueAnalysisAuth.mockRejectedValue(error);

    await expect(
      finishAnalysisAuthentication({
        ...BASE_OPTIONS,
        initialResponse: {
          status: 'AUTH_PENDING',
          nextAction: 'SIMPLE_AUTH',
        },
      }),
    ).rejects.toBe(error);
    expect(getAnalysisRequest).not.toHaveBeenCalled();
  });

  it('일시 오류 후 상태 조회도 실패하면 원래 오류를 전파한다', async () => {
    const originalError = { response: { status: 502 } };
    continueAnalysisAuth.mockRejectedValue(originalError);
    getAnalysisRequest.mockRejectedValue(new Error('status lookup failed'));

    await expect(
      finishAnalysisAuthentication({
        ...BASE_OPTIONS,
        initialResponse: {
          status: 'AUTH_PENDING',
          nextAction: 'SIMPLE_AUTH',
        },
      }),
    ).rejects.toBe(originalError);
  });

  it('알 수 없는 인증 상태를 거부한다', async () => {
    await expect(
      finishAnalysisAuthentication({
        ...BASE_OPTIONS,
        initialResponse: { status: 'UNKNOWN' },
      }),
    ).rejects.toThrow('추가 인증 상태를 확인하지 못했습니다.');
  });

  it('제한된 인증 전환 횟수를 넘기면 timeout 오류를 발생시킨다', async () => {
    continueAnalysisAuth.mockResolvedValue({
      status: 'AUTH_PENDING',
      nextAction: 'SIMPLE_AUTH',
    });

    await expect(
      finishAnalysisAuthentication({
        ...BASE_OPTIONS,
        initialResponse: {
          status: 'AUTH_PENDING',
          nextAction: 'SIMPLE_AUTH',
        },
      }),
    ).rejects.toThrow('인증 대기 시간이 초과되었습니다.');
    expect(continueAnalysisAuth).toHaveBeenCalledTimes(30);
  });
});
