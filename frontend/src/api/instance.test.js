import { beforeEach, describe, expect, it, vi } from 'vitest';

import api, { AUTH_UNAUTHORIZED_EVENT } from './instance';
import { getAccessToken, removeAccessToken } from './token';

const { requestUse, responseUse } = vi.hoisted(() => ({
  requestUse: vi.fn(),
  responseUse: vi.fn(),
}));

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      interceptors: {
        request: { use: requestUse },
        response: { use: responseUse },
      },
    })),
  },
}));

vi.mock('./token', () => ({
  getAccessToken: vi.fn(),
  removeAccessToken: vi.fn(),
}));

const handleError = responseUse.mock.calls[0][1];
const handleResponse = responseUse.mock.calls[0][0];
const handleRequest = requestUse.mock.calls[0][0];

describe('API response interceptor', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    getAccessToken.mockReset();
    removeAccessToken.mockClear();
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

  it.each([401, 403])('%i 응답이면 인증 정보를 정리하고 로그인 이동 이벤트를 발생시킨다', async (status) => {
    const error = { response: { status } };
    const dispatchEvent = vi.spyOn(window, 'dispatchEvent');

    await expect(handleError(error)).rejects.toBe(error);

    expect(removeAccessToken).toHaveBeenCalledOnce();
    expect(dispatchEvent).toHaveBeenCalledWith(
      expect.objectContaining({ type: AUTH_UNAUTHORIZED_EVENT }),
    );
  });

  it('401/403이 아닌 오류는 인증 상태를 변경하지 않는다', async () => {
    const error = { response: { status: 500 } };
    const dispatchEvent = vi.spyOn(window, 'dispatchEvent');

    await expect(handleError(error)).rejects.toBe(error);

    expect(removeAccessToken).not.toHaveBeenCalled();
    expect(dispatchEvent).not.toHaveBeenCalled();
  });

  it('API 인스턴스를 생성한다', () => {
    expect(api).toBeDefined();
  });
});
