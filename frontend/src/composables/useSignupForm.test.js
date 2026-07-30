import { describe, expect, it } from 'vitest';

import { AUTH_MESSAGE } from '@/constants/auth/authMessage';
import { useSignupForm } from './useSignupForm';

const fillValidForm = (form) => {
  form.nickname = '길동';
  form.email = 'user@example.com';
  form.password = 'Password1!';
  form.passwordConfirm = 'Password1!';
};

describe('useSignupForm', () => {
  it('회원가입 폼의 초기값을 제공한다', () => {
    const { form } = useSignupForm();

    expect(form).toMatchObject({
      characterType: 'CAT',
      email: '',
      password: '',
      passwordConfirm: '',
      nickname: '',
    });
    expect(form.termConsents).toEqual([
      { agreed: true, termId: 1 },
      { agreed: true, termId: 2 },
    ]);
  });

  it('입력 전에는 기본 상태와 안내 문구를 반환한다', () => {
    const { getMessage, getStatus } = useSignupForm();

    expect(getStatus('email')).toBe('default');
    expect(getMessage('email')).toBe(AUTH_MESSAGE.DEF_EMAIL);
    expect(getMessage('nickname')).toBe(AUTH_MESSAGE.NICKNAME);
    expect(getStatus('unknown')).toBe('default');
    expect(getMessage('unknown')).toBe('');
  });

  it('입력을 시작한 필드의 검증 상태와 문구를 갱신한다', () => {
    const { form, getMessage, getStatus, handleFieldInput } = useSignupForm();

    form.email = 'wrong-email';
    handleFieldInput('email', form.email);
    expect(getStatus('email')).toBe('wrong');
    expect(getMessage('email')).toBe(AUTH_MESSAGE.WRO_EMAIL);

    form.email = 'user@example.com';
    expect(getStatus('email')).toBe('correct');
    expect(getMessage('email')).toBe(AUTH_MESSAGE.COR_EMAIL);
  });

  it('빈 입력은 필드 검증을 시작하지 않는다', () => {
    const { getStatus, handleFieldInput } = useSignupForm();

    handleFieldInput('nickname', '');

    expect(getStatus('nickname')).toBe('default');
  });

  it('모든 필드를 시작 상태로 전환한다', () => {
    const { getStatus, startAllFields } = useSignupForm();

    startAllFields();

    expect(getStatus('nickname')).toBe('wrong');
    expect(getStatus('email')).toBe('wrong');
    expect(getStatus('password')).toBe('wrong');
    expect(getStatus('passwordConfirm')).toBe('wrong');
  });

  it('모든 입력이 유효한 경우에만 폼을 유효하다고 판단한다', () => {
    const { form, isFormValid } = useSignupForm();

    expect(isFormValid()).toBe(false);
    fillValidForm(form);
    expect(isFormValid()).toBe(true);

    form.passwordConfirm = 'Different1!';
    expect(isFormValid()).toBe(false);
  });
});
