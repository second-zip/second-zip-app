import { afterEach, describe, expect, it, vi } from 'vitest';

import {
  getAnalysisAuthPayload,
  normalizeAnalysisRequest,
  requireResponseValue,
  wait,
} from './analysisFlow';

describe('analysis flow utilities', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.useRealTimers();
  });

  it('인증 환경변수를 trim해 요청 payload로 구성한다', () => {
    vi.stubEnv('VITE_ANALYSIS_TEST_BIRTH_DATE', '  TEST_BIRTH  ');
    vi.stubEnv('VITE_ANALYSIS_TEST_PHONE_NO', ' TEST_PHONE ');
    vi.stubEnv('VITE_ANALYSIS_TEST_USER_NAME', ' TEST_USER ');

    expect(getAnalysisAuthPayload()).toEqual({
      birthDate: 'TEST_BIRTH',
      consent: true,
      phoneNo: 'TEST_PHONE',
      provider: 'KAKAO',
      telecom: 'SKT',
      userName: 'TEST_USER',
    });
  });

  it('필수 인증 환경변수가 없으면 userMessage가 있는 오류를 발생시킨다', () => {
    vi.stubEnv('VITE_ANALYSIS_TEST_BIRTH_DATE', '');
    vi.stubEnv('VITE_ANALYSIS_TEST_PHONE_NO', 'TEST_PHONE');
    vi.stubEnv('VITE_ANALYSIS_TEST_USER_NAME', 'TEST_USER');

    expect(getAnalysisAuthPayload).toThrowError('분석 인증 설정을 확인해주세요.');
    try {
      getAnalysisAuthPayload();
    } catch (error) {
      expect(error.userMessage).toBe('분석 인증 설정을 확인해주세요.');
    }
  });

  it('실제 분석 요청을 정규화하고 deposit을 숫자로 변환한다', () => {
    expect(
      normalizeAnalysisRequest({
        roadAddress: '  서울 강남구  ',
        detailAddress: ' 101동 1203호 ',
        deposit: '100000000',
      }),
    ).toEqual({
      roadAddress: '서울 강남구',
      detailAddress: '101동 1203호',
      deposit: 100_000_000,
    });
  });

  it.each([
    [{ roadAddress: '', detailAddress: '', deposit: 1 }],
    [{ roadAddress: '서울', detailAddress: '', deposit: 'not-a-number' }],
  ])('잘못된 분석 요청 %j을 거부한다', (request) => {
    expect(() => normalizeAnalysisRequest(request)).toThrowError(
      '입력한 분석 정보를 확인해주세요.',
    );
  });

  it('response 필수값과 대기 유틸을 처리한다', async () => {
    expect(requireResponseValue(0, 'error')).toBe(0);
    expect(() => requireResponseValue('', 'required')).toThrowError('required');

    vi.useFakeTimers();
    let finished = false;
    const pending = wait(100).then(() => {
      finished = true;
    });
    await vi.advanceTimersByTimeAsync(99);
    expect(finished).toBe(false);
    await vi.advanceTimersByTimeAsync(1);
    await pending;
    expect(finished).toBe(true);
  });
});
