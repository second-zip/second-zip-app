import { describe, expect, it } from 'vitest';

import { getApiError, getApiErrorMessage } from './error';

describe('getApiError', () => {
  it('백엔드 ErrorResponseDTO의 code와 message를 반환한다', () => {
    const error = {
      response: {
        data: {
          code: 'COMMON_502',
          message: 'CODEF 추가인증 요청에 실패했습니다.',
        },
      },
    };

    expect(getApiError(error)).toEqual({
      code: 'COMMON_502',
      message: 'CODEF 추가인증 요청에 실패했습니다.',
    });
  });

  it('axios timeout 오류를 구분한다', () => {
    expect(getApiError({ code: 'ECONNABORTED' })).toEqual({
      code: 'ECONNABORTED',
      message: '요청 시간이 초과되었습니다. 다시 시도해주세요.',
    });
  });

  it('error 필드를 code로 사용하고 message 편의 함수를 제공한다', () => {
    const error = {
      response: { data: { error: 'LEGACY_CODE', message: '요청 실패' } },
    };

    expect(getApiError(error).code).toBe('LEGACY_CODE');
    expect(getApiErrorMessage(error)).toBe('요청 실패');
  });

  it('응답이 없는 오류에 공통 fallback을 반환한다', () => {
    expect(getApiError({})).toEqual({
      code: 'UNKNOWN_ERROR',
      message: '요청을 처리하는 중 오류가 발생했습니다.',
    });
  });
});
