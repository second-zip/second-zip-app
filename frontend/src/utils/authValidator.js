/**
 * 닉네임 형식
 * - 한글 또는 영문만 사용
 * - 한글과 영문 혼합 가능
 * - 2자 이상 20자 이하
 */
const NICKNAME_REGEX = /^[가-힣A-Za-z]{2,20}$/;

/**
 * 이메일 형식
 * 예: user@example.com
 */
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * 비밀번호 형식
 * - 영문 1자 이상
 * - 숫자 1자 이상
 * - 특수문자 1자 이상
 * - 8자 이상 16자 이하
 * - 허용 특수문자: ! @ # $ % ^ & *
 */
const PASSWORD_REGEX =
  /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,16}$/;

export const isValidNickname = (value = '') => {
  return NICKNAME_REGEX.test(value.trim());
};

export const isValidEmail = (value = '') => {
  return EMAIL_REGEX.test(value.trim());
};

export const isValidPassword = (value = '') => {
  return PASSWORD_REGEX.test(value);
};

export const isPasswordConfirmed = (password = '', passwordConfirm = '') => {
  return passwordConfirm.length > 0 && password === passwordConfirm;
};
