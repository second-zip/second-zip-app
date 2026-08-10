import { beforeEach, describe, expect, it, vi } from 'vitest';

import api from './instance';
import {
  getMyAccount,
  updateCharacter,
  updateMyAccount,
  updatePassword,
  withdraw,
} from './user';

vi.mock('./instance', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('회원정보 API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('GET /user로 내 회원정보를 조회한다', async () => {
    api.get.mockResolvedValue({ data: { accountId: 1 } });
    await expect(getMyAccount()).resolves.toEqual({ accountId: 1 });
    expect(api.get).toHaveBeenCalledWith('/user');
  });

  it('PATCH /user로 닉네임을 변경한다', async () => {
    api.patch.mockResolvedValue({ data: { nickname: '새닉네임' } });
    await updateMyAccount({ nickname: '새닉네임' });
    expect(api.patch).toHaveBeenCalledWith('/user', { nickname: '새닉네임' });
  });

  it('PATCH /user/character로 캐릭터를 변경한다', async () => {
    api.patch.mockResolvedValue({ data: { characterType: 'WOMAN' } });
    await updateCharacter({ characterType: 'WOMAN' });
    expect(api.patch).toHaveBeenCalledWith('/user/character', { characterType: 'WOMAN' });
  });

  it('PATCH /user/password로 비밀번호를 변경한다', async () => {
    const body = {
      currentPassword: 'password1!',
      newPassword: 'newPassword1!',
      newPasswordConfirm: 'newPassword1!',
    };
    api.patch.mockResolvedValue({ data: { message: '변경 완료' } });
    await updatePassword(body);
    expect(api.patch).toHaveBeenCalledWith('/user/password', body);
  });

  it('DELETE /user 요청 본문에 현재 비밀번호를 전달한다', async () => {
    api.delete.mockResolvedValue({ data: { message: '탈퇴 완료' } });
    await withdraw({ password: 'password1!' });
    expect(api.delete).toHaveBeenCalledWith('/user', {
      data: { password: 'password1!' },
    });
  });
});
