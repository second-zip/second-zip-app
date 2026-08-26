import { beforeEach, describe, expect, it, vi } from 'vitest';

import api, { AUTH_UNAUTHORIZED_EVENT } from './instance';
import {
  getAccessToken,
  getRefreshToken,
  removeAccessToken,
  removeRefreshToken,
  setAccessToken,
} from './token';
import { reissueAccessToken } from './tokenReissue';

const { apiRequest, requestUse, responseUse } = vi.hoisted(() => ({
  apiRequest: vi.fn(),
  requestUse: vi.fn(),
  responseUse: vi.fn(),
}));

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      request: apiRequest,
      interceptors: {
        request: { use: requestUse },
        response: { use: responseUse },
      },
    })),
  },
}));

vi.mock('./token', () => ({
  getAccessToken: vi.fn(),
  getRefreshToken: vi.fn(),
  removeAccessToken: vi.fn(),
  removeRefreshToken: vi.fn(),
  setAccessToken: vi.fn(),
}));
vi.mock('./tokenReissue', () => ({ reissueAccessToken: vi.fn() }));

const handleError = responseUse.mock.calls[0][1];
const handleResponse = responseUse.mock.calls[0][0];
const handleRequest = requestUse.mock.calls[0][0];

describe('API response interceptor', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    getAccessToken.mockReset();
    getRefreshToken.mockReset();
    removeAccessToken.mockClear();
    removeRefreshToken.mockClear();
    setAccessToken.mockClear();
    reissueAccessToken.mockReset();
    apiRequest.mockReset();
  });

  it('저장된 토큰을 Authorization 헤더에 추가한다', () => {
    getAccessToken.mockReturnValue('access-token');
    const config = { headers: { set: vi.fn() } };

    expect(handleRequest(config)).toBe(config);
    expect(config.headers.set).toHaveBeenCalledWith(
      'Authorization',
      'Bearer access-token',
    );
  });

  it('토큰이 없으면 Authorization 헤더를 추가하지 않는다', () => {
    const config = { headers: { set: vi.fn() } };

    expect(handleRequest(config)).toBe(config);
    expect(config.headers.set).not.toHaveBeenCalled();
  });

  it('성공 응답을 그대로 반환한다', () => {
    const response = { data: { ok: true } };

    expect(handleResponse(response)).toBe(response);
  });

  it('401 응답이면 Access Token을 재발급하고 원 요청을 재시도한다', async () => {
    const retriedResponse = { data: { ok: true } };
    const config = { headers: { set: vi.fn() }, url: '/users/me' };
    const error = { config, response: { status: 401 } };
    getRefreshToken.mockReturnValue('refresh-token');
    reissueAccessToken.mockResolvedValue({ accessToken: 'new-access-token' });
    apiRequest.mockResolvedValue(retriedResponse);

    await expect(handleError(error)).resolves.toBe(retriedResponse);

    expect(reissueAccessToken).toHaveBeenCalledWith('refresh-token');
    expect(setAccessToken).toHaveBeenCalledWith('new-access-token');
    expect(config.headers.set).toHaveBeenCalledWith(
      'Authorization',
      'Bearer new-access-token',
    );
    expect(config._retry).toBe(true);
    expect(apiRequest).toHaveBeenCalledWith(config);
    expect(removeAccessToken).not.toHaveBeenCalled();
  });

  it('동시에 발생한 401은 하나의 재발급 요청을 공유한다', async () => {
    let resolveReissue;
    const reissuePromise = new Promise((resolve) => {
      resolveReissue = resolve;
    });
    getRefreshToken.mockReturnValue('refresh-token');
    reissueAccessToken.mockReturnValue(reissuePromise);
    apiRequest.mockResolvedValue({ data: { ok: true } });

    const first = handleError({
      config: { headers: {}, url: '/users/me' },
      response: { status: 401 },
    });
    const second = handleError({
      config: { headers: {}, url: '/analysis-reports' },
      response: { status: 401 },
    });

    expect(reissueAccessToken).toHaveBeenCalledOnce();
    resolveReissue({ accessToken: 'new-access-token' });
    await Promise.all([first, second]);

    expect(apiRequest).toHaveBeenCalledTimes(2);
  });

  it('재발급 실패 시 두 토큰을 지우고 로그인 이동 이벤트를 발생시킨다', async () => {
    const error = {
      config: { headers: {}, url: '/users/me' },
      response: { status: 401 },
    };
    const dispatchEvent = vi.spyOn(window, 'dispatchEvent');
    getRefreshToken.mockReturnValue('invalid-refresh-token');
    reissueAccessToken.mockRejectedValue(new Error('reissue failed'));

    await expect(handleError(error)).rejects.toBe(error);

    expect(removeAccessToken).toHaveBeenCalledOnce();
    expect(removeRefreshToken).toHaveBeenCalledOnce();
    expect(dispatchEvent).toHaveBeenCalledWith(
      expect.objectContaining({ type: AUTH_UNAUTHORIZED_EVENT }),
    );
  });

  it('403 오류는 재발급 없이 인증 정보를 정리하고 로그인 이동 이벤트를 발생시킨다', async () => {
    const error = {
      config: { url: '/users/me' },
      response: { status: 403 },
    };
    const dispatchEvent = vi.spyOn(window, 'dispatchEvent');

    await expect(handleError(error)).rejects.toBe(error);

    expect(reissueAccessToken).not.toHaveBeenCalled();
    expect(removeAccessToken).toHaveBeenCalledOnce();
    expect(removeRefreshToken).toHaveBeenCalledOnce();
    expect(dispatchEvent).toHaveBeenCalledWith(
      expect.objectContaining({ type: AUTH_UNAUTHORIZED_EVENT }),
    );
  });

  it('401/403이 아닌 오류는 재발급하거나 인증 상태를 변경하지 않는다', async () => {
    const error = {
      config: { url: '/users/me' },
      response: { status: 500 },
    };
    const dispatchEvent = vi.spyOn(window, 'dispatchEvent');

    await expect(handleError(error)).rejects.toBe(error);

    expect(reissueAccessToken).not.toHaveBeenCalled();
    expect(removeAccessToken).not.toHaveBeenCalled();
    expect(removeRefreshToken).not.toHaveBeenCalled();
    expect(dispatchEvent).not.toHaveBeenCalled();
  });

  it('로그인 401은 재발급이나 강제 로그아웃을 실행하지 않는다', async () => {
    const error = {
      config: { url: '/auth/login' },
      response: { status: 401 },
    };
    const dispatchEvent = vi.spyOn(window, 'dispatchEvent');

    await expect(handleError(error)).rejects.toBe(error);

    expect(reissueAccessToken).not.toHaveBeenCalled();
    expect(removeAccessToken).not.toHaveBeenCalled();
    expect(dispatchEvent).not.toHaveBeenCalled();
  });

  it('Refresh Token이 없으면 인증 정보를 정리한다', async () => {
    const error = {
      config: { headers: {}, url: '/users/me' },
      response: { status: 401 },
    };
    getRefreshToken.mockReturnValue(null);

    await expect(handleError(error)).rejects.toBe(error);

    expect(removeAccessToken).toHaveBeenCalledOnce();
    expect(removeRefreshToken).toHaveBeenCalledOnce();
    expect(reissueAccessToken).not.toHaveBeenCalled();
  });

  it('재시도한 요청도 401이면 다시 재발급하지 않고 인증 정보를 정리한다', async () => {
    const error = {
      config: { _retry: true, url: '/users/me' },
      response: { status: 401 },
    };

    await expect(handleError(error)).rejects.toBe(error);

    expect(reissueAccessToken).not.toHaveBeenCalled();
    expect(removeAccessToken).toHaveBeenCalledOnce();
    expect(removeRefreshToken).toHaveBeenCalledOnce();
  });

  it('API 인스턴스를 생성한다', () => {
    expect(api).toBeDefined();
  });
});
