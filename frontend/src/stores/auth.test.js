import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { login as loginApi, logout as logoutApi, signup as signupApi } from '@/api/auth';
import { getAccessToken, removeAccessToken, setAccessToken } from '@/api/token';
import { getMyAccount } from '@/api/user';
import { useAuthStore } from './auth';

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  signup: vi.fn(),
}));
vi.mock('@/api/token', () => ({
  getAccessToken: vi.fn(),
  removeAccessToken: vi.fn(),
  setAccessToken: vi.fn(),
}));
vi.mock('@/api/user', () => ({
  getMyAccount: vi.fn(),
}));

describe('auth store', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAccessToken.mockReturnValue(null);
    setActivePinia(createPinia());
  });

  it('저장된 토큰으로 인증 상태를 초기화한다', () => {
    getAccessToken.mockReturnValue('saved-token');

    const store = useAuthStore();

    expect(store.isAuthenticated).toBe(true);
  });

  it('회원가입 결과를 반환하고 loading을 복원한다', async () => {
    const response = { accountId: 1 };
    signupApi.mockResolvedValue(response);
    const store = useAuthStore();

    await expect(store.signup({ email: 'new@example.com' })).resolves.toBe(response);
    expect(store.loading).toBe(false);
  });

  it('회원가입 실패 시에도 loading을 복원한다', async () => {
    signupApi.mockRejectedValue(new Error('signup failed'));
    const store = useAuthStore();

    await expect(store.signup({})).rejects.toThrow('signup failed');
    expect(store.loading).toBe(false);
  });

  it('로그인 성공 시 토큰과 사용자 정보를 저장한다', async () => {
    const loginResult = {
      accessToken: 'new-token',
      accountId: 1,
      characterType: 'CAT',
      email: 'user@example.com',
      nickname: '길동',
    };
    loginApi.mockResolvedValue(loginResult);
    const store = useAuthStore();

    await expect(store.login({ email: loginResult.email })).resolves.toEqual({
      accountId: 1,
      characterType: 'CAT',
      email: 'user@example.com',
      nickname: '길동',
    });
    expect(setAccessToken).toHaveBeenCalledWith('new-token');
    expect(store.isAuthenticated).toBe(true);
    expect(store.loading).toBe(false);
  });

  it('로그인 실패 시 loading을 복원하고 인증되지 않은 상태를 유지한다', async () => {
    loginApi.mockRejectedValue(new Error('login failed'));
    const store = useAuthStore();

    await expect(store.login({})).rejects.toThrow('login failed');
    expect(store.loading).toBe(false);
    expect(store.isAuthenticated).toBe(false);
  });

  it('최신 회원정보를 조회하고 myPageLoading을 복원한다', async () => {
    const account = { accountId: 1, nickname: '새닉네임' };
    getMyAccount.mockResolvedValue(account);
    const store = useAuthStore();

    await expect(store.fetchMyPage()).resolves.toEqual(account);
    expect(store.myPage).toEqual(account);
    expect(store.myPageLoading).toBe(false);
  });

  it('회원정보 조회 실패 시에도 myPageLoading을 복원한다', async () => {
    getMyAccount.mockRejectedValue(new Error('fetch failed'));
    const store = useAuthStore();

    await expect(store.fetchMyPage()).rejects.toThrow('fetch failed');
    expect(store.myPageLoading).toBe(false);
  });

  it('인증 정보를 초기화한다', async () => {
    loginApi.mockResolvedValue({
      accessToken: 'new-token',
      accountId: 1,
      characterType: 'CAT',
      email: 'user@example.com',
      nickname: '길동',
    });
    const store = useAuthStore();
    await store.login({});

    store.clearAuth();

    expect(removeAccessToken).toHaveBeenCalledOnce();
    expect(store.isAuthenticated).toBe(false);
    expect(store.myPage).toBeNull();
  });

  it('서버 로그아웃 성공 후 클라이언트 인증 정보를 초기화한다', async () => {
    logoutApi.mockResolvedValue(undefined);
    const store = useAuthStore();

    await store.logout();

    expect(logoutApi).toHaveBeenCalledOnce();
    expect(removeAccessToken).toHaveBeenCalledOnce();
    expect(store.loading).toBe(false);
  });

  it('서버 로그아웃 실패 시에도 클라이언트 인증 정보를 초기화한다', async () => {
    logoutApi.mockRejectedValue(new Error('logout failed'));
    const store = useAuthStore();

    await expect(store.logout()).rejects.toThrow('logout failed');
    expect(removeAccessToken).toHaveBeenCalledOnce();
    expect(store.loading).toBe(false);
  });
});
