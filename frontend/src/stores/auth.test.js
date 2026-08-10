import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { login as loginApi, logout as logoutApi, signup as signupApi } from '@/api/auth';
import { getAccessToken, removeAccessToken, setAccessToken } from '@/api/token';
import {
  getMyAccount,
  updateCharacter,
  updateMyAccount,
  updatePassword,
  withdraw,
} from '@/api/user';
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
  updateCharacter: vi.fn(),
  updateMyAccount: vi.fn(),
  updatePassword: vi.fn(),
  withdraw: vi.fn(),
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

  it('캐릭터를 변경하고 최신 회원 정보를 store에 반영한다', async () => {
    const account = { accountId: 1, characterType: 'WOMAN' };
    updateCharacter.mockResolvedValue(account);
    const store = useAuthStore();

    await expect(store.changeCharacter('WOMAN')).resolves.toEqual(account);
    expect(updateCharacter).toHaveBeenCalledWith({ characterType: 'WOMAN' });
    expect(store.myPage).toEqual(account);
  });

  it('캐릭터 변경에 실패하면 기존 회원 정보를 유지한다', async () => {
    const store = useAuthStore();
    const existingAccount = { accountId: 1, characterType: 'CAT' };
    getMyAccount.mockResolvedValue(existingAccount);
    updateCharacter.mockRejectedValue(new Error('update failed'));
    await store.fetchMyPage();

    await expect(store.changeCharacter('MAN')).rejects.toThrow('update failed');
    expect(store.myPage).toEqual(existingAccount);
  });
  it('회원정보 수정 응답을 Store에 반영한다', async () => {
    const account = { accountId: 1, nickname: '새닉네임' };
    updateMyAccount.mockResolvedValue(account);
    const store = useAuthStore();

    await expect(store.updateProfile('새닉네임')).resolves.toEqual(account);
    expect(updateMyAccount).toHaveBeenCalledWith({ nickname: '새닉네임' });
    expect(store.myPage).toEqual(account);
  });

  it('비밀번호 변경 성공 시 인증정보를 지운다', async () => {
    updatePassword.mockResolvedValue({ message: '변경 완료' });
    const store = useAuthStore();

    await store.changePassword({ currentPassword: 'old', newPassword: 'new' });

    expect(removeAccessToken).toHaveBeenCalledOnce();
    expect(store.isAuthenticated).toBe(false);
    expect(store.loading).toBe(false);
  });

  it('회원탈퇴 실패 시 로그인 상태를 유지한다', async () => {
    getAccessToken.mockReturnValue('saved-token');
    withdraw.mockRejectedValue(new Error('wrong password'));
    const store = useAuthStore();

    await expect(store.withdraw('wrong')).rejects.toThrow('wrong password');

    expect(removeAccessToken).not.toHaveBeenCalled();
    expect(store.isAuthenticated).toBe(true);
  });

  it('회원탈퇴 성공 시 인증정보를 지운다', async () => {
    getAccessToken.mockReturnValue('saved-token');
    withdraw.mockResolvedValue({ message: '탈퇴 완료' });
    const store = useAuthStore();

    await store.withdraw('password1!');

    expect(withdraw).toHaveBeenCalledWith({ password: 'password1!' });
    expect(removeAccessToken).toHaveBeenCalledOnce();
    expect(store.isAuthenticated).toBe(false);
  });
});
