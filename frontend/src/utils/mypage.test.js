import { describe, expect, it } from 'vitest';
import {
  isValidNickname,
  isValidPassword,
  summarizeReports,
  withObjectParticle,
} from './mypage';

describe('mypage utils', () => {
  it('리포트 응답의 위험도를 집계한다', () => {
    expect(summarizeReports({ totalCount: 3, reports: [
      { result: 'SAFE' }, { result: 'CAUTION' }, { result: 'DANGER' },
    ] })).toEqual({ total: 3, safe: 1, caution: 1, danger: 1 });
  });

  it('닉네임과 비밀번호 규칙을 검증한다', () => {
    expect(isValidNickname('집집')).toBe(true);
    expect(isValidNickname('집')).toBe(false);
    expect(isValidPassword('password1!')).toBe(true);
    expect(isValidPassword('password')).toBe(false);
  });

  it('캐릭터 이름의 받침에 맞는 목적격 조사를 붙인다', () => {
    expect(withObjectParticle('냥냥이')).toBe('냥냥이를');
    expect(withObjectParticle('엘리스')).toBe('엘리스를');
    expect(withObjectParticle('위장남사친')).toBe('위장남사친을');
  });
});
