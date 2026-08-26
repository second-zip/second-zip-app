import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  getAccessToken,
  getRefreshToken,
  removeAccessToken,
  removeRefreshToken,
  setAccessToken,
  setRefreshToken,
} from './token';

describe('인증 토큰', () => {
  const storage = new Map();

  beforeEach(() => {
    vi.stubGlobal('localStorage', {
      clear: () => storage.clear(),
      getItem: (key) => storage.get(key) ?? null,
      removeItem: (key) => storage.delete(key),
      setItem: (key, value) => storage.set(key, String(value)),
    });
    localStorage.clear();
  });

  it('토큰을 저장하고 조회한다', () => {
    setAccessToken('access-token');

    expect(getAccessToken()).toBe('access-token');
  });

  it('빈 토큰은 저장하지 않는다', () => {
    setAccessToken('');

    expect(getAccessToken()).toBeNull();
  });

  it('저장된 토큰을 제거한다', () => {
    localStorage.setItem('accessToken', 'access-token');

    removeAccessToken();

    expect(getAccessToken()).toBeNull();
  });

  it('Refresh Token을 저장하고 조회한다', () => {
    setRefreshToken('refresh-token');

    expect(getRefreshToken()).toBe('refresh-token');
  });

  it('빈 Refresh Token은 저장하지 않는다', () => {
    setRefreshToken('');

    expect(getRefreshToken()).toBeNull();
  });

  it('저장된 Refresh Token을 제거한다', () => {
    localStorage.setItem('refreshToken', 'refresh-token');

    removeRefreshToken();

    expect(getRefreshToken()).toBeNull();
  });
});
