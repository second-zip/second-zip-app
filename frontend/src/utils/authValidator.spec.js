import { describe, expect, it } from 'vitest';

import {
  isPasswordConfirmed,
  isValidEmail,
  isValidNickname,
  isValidPassword,
} from './authValidator';

describe('auth validator', () => {
  it.each(['길동', 'John', '길동John', '  길동  '])(
    '유효한 닉네임을 허용한다: %s',
    (nickname) => expect(isValidNickname(nickname)).toBe(true),
  );

  it.each(['', '김', 'user1', '사용자!', '가'.repeat(21)])(
    '유효하지 않은 닉네임을 거부한다: %s',
    (nickname) => expect(isValidNickname(nickname)).toBe(false),
  );

  it.each(['user@example.com', ' user@example.com '])(
    '유효한 이메일을 허용한다: %s',
    (email) => expect(isValidEmail(email)).toBe(true),
  );

  it.each(['', 'user', 'user@', '@example.com', 'user @example.com'])(
    '유효하지 않은 이메일을 거부한다: %s',
    (email) => expect(isValidEmail(email)).toBe(false),
  );

  it.each(['Password1!', 'abc12345@', 'A1!aaaaa'])(
    '조건에 맞는 비밀번호를 허용한다: %s',
    (password) => expect(isValidPassword(password)).toBe(true),
  );

  it.each([
    '',
    'short1!',
    'password!',
    'password1',
    '12345678!',
    'Password1?',
    'A1!'.padEnd(17, 'a'),
  ])('조건에 맞지 않는 비밀번호를 거부한다: %s', (password) => {
    expect(isValidPassword(password)).toBe(false);
  });

  it('비밀번호 확인 값이 존재하고 원본과 같을 때만 일치한다', () => {
    expect(isPasswordConfirmed('Password1!', 'Password1!')).toBe(true);
    expect(isPasswordConfirmed('Password1!', 'different')).toBe(false);
    expect(isPasswordConfirmed('', '')).toBe(false);
  });
});
