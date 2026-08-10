import { describe, expect, it } from 'vitest';

import { getContinuePayload, isRecoverableAuthError } from './analysisAuth';

const AUTH_PAYLOAD = { userName: 'TEST_USER' };
const ANALYSIS_REQUEST = {
  roadAddress: '서울특별시 강남구 테헤란로 1',
  detailAddress: '202동 303호',
};

const selectionResponse = (nextAction, selectionOptions) => ({
  status: 'SELECTION_REQUIRED',
  nextAction,
  selectionOptions,
});

describe('analysis auth utilities', () => {
  it.each([
    [
      'ADDRESS_SELECTION',
      [
        { value: 'other', label: '부산광역시 해운대구' },
        {
          value: 'address-id',
          label: '서울특별시 강남구 테헤란로 1',
        },
      ],
      'address-id',
    ],
    [
      'DONG_SELECTION',
      [{ value: '101', label: '101동' }, { value: '202', label: '202동' }],
      '202',
    ],
    [
      'HO_SELECTION',
      [{ value: '303', label: '303호' }],
      '303',
    ],
  ])('%s에 실제 주소 값과 일치하는 selectionValue를 선택한다', (
    nextAction,
    options,
    expected,
  ) => {
    expect(
      getContinuePayload(
        selectionResponse(nextAction, options),
        AUTH_PAYLOAD,
        ANALYSIS_REQUEST,
      ),
    ).toEqual({ authentication: AUTH_PAYLOAD, selectionValue: expected });
  });

  it('SIMPLE_AUTH는 인증 payload만 전달한다', () => {
    expect(
      getContinuePayload(
        { status: 'AUTH_PENDING', nextAction: 'SIMPLE_AUTH' },
        AUTH_PAYLOAD,
        ANALYSIS_REQUEST,
      ),
    ).toEqual({ authentication: AUTH_PAYLOAD });
  });

  it.each([
    [selectionResponse('DONG_SELECTION', []), '추가 인증 선택값'],
    [{ nextAction: 'CAPTCHA' }, '보안문자 입력'],
    [{ nextAction: 'UNKNOWN' }, '추가 인증 상태'],
  ])('처리할 수 없는 추가 인증 %j을 거부한다', (response, message) => {
    expect(() =>
      getContinuePayload(response, AUTH_PAYLOAD, ANALYSIS_REQUEST),
    ).toThrowError(expect.objectContaining({ message: expect.stringContaining(message) }));
  });

  it('타임아웃·네트워크·일시적 HTTP 오류만 복구 가능하게 분류한다', () => {
    expect(isRecoverableAuthError({ code: 'ECONNABORTED' })).toBe(true);
    expect(isRecoverableAuthError(new Error('network'))).toBe(true);
    expect(isRecoverableAuthError({ response: { status: 503 } })).toBe(true);
    expect(isRecoverableAuthError({ response: { status: 400 } })).toBe(false);
  });
});
