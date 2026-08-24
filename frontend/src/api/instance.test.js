import { beforeEach, describe, expect, it, vi } from 'vitest';

import api, { AUTH_UNAUTHORIZED_EVENT } from './instance';
import { removeAccessToken } from './token';

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

describe('API response interceptor', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    removeAccessToken.mockClear();
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
