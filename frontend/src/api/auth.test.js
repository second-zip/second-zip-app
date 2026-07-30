import { beforeEach, describe, expect, it, vi } from 'vitest';

import api from './instance';
import { login, logout, signup } from './auth';

vi.mock('./instance', () => ({
  default: {
    post: vi.fn(),
  },
}));

describe('auth API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it.each([
    ['signup', signup, '/auth/signup', { email: 'new@example.com' }],
    ['login', login, '/auth/login', { email: 'user@example.com' }],
  ])('%s 요청의 응답 data를 반환한다', async (_, request, url, body) => {
    const data = { success: true };
    api.post.mockResolvedValue({ data });

    await expect(request(body)).resolves.toBe(data);
    expect(api.post).toHaveBeenCalledWith(url, body);
  });

  it('로그아웃 요청의 응답 data를 반환한다', async () => {
    const data = { message: '로그아웃되었습니다.' };
    api.post.mockResolvedValue({ data });

    await expect(logout()).resolves.toBe(data);
    expect(api.post).toHaveBeenCalledWith('/auth/logout');
  });
});
