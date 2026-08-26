import { beforeEach, describe, expect, it, vi } from 'vitest';

import { reissueAccessToken } from './tokenReissue';

const { post } = vi.hoisted(() => ({ post: vi.fn() }));

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({ post })),
  },
}));

describe('token reissue API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('Refresh Token으로 Access Token 재발급을 요청한다', async () => {
    const data = { accessToken: 'new-access-token' };
    post.mockResolvedValue({ data });

    await expect(reissueAccessToken('refresh-token')).resolves.toBe(data);
    expect(post).toHaveBeenCalledWith('/auth/token/reissue', {
      refreshToken: 'refresh-token',
    });
  });
});
