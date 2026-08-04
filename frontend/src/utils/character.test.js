import { describe, expect, it } from 'vitest';

import { normalizeCharacterType } from './character';

describe('normalizeCharacterType', () => {
  it.each([
    ['CAT', 'cat'],
    [' man ', 'man'],
    ['WOMAN', 'woman'],
  ])('%s 값을 %s 캐릭터로 정규화한다', (input, expected) => {
    expect(normalizeCharacterType(input)).toBe(expected);
  });

  it('지원하지 않는 값은 고양이로 처리한다', () => {
    expect(normalizeCharacterType('UNKNOWN')).toBe('cat');
    expect(normalizeCharacterType()).toBe('cat');
  });

  it('호출 화면에서 별도 fallback을 지정할 수 있다', () => {
    expect(normalizeCharacterType('UNKNOWN', null)).toBeNull();
  });
});
