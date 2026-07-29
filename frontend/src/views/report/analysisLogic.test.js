import assert from 'node:assert/strict';
import test from 'node:test';

import {
  formatKoreanDeposit,
  selectSecretaryValue,
} from './analysisLogic.js';

test('보증금을 억·만원 단위 한글 금액으로 표시한다', () => {
  assert.equal(formatKoreanDeposit(150_000_000), '1억 5000만');
  assert.equal(formatKoreanDeposit(220_000_000), '2억 2000만');
  assert.equal(formatKoreanDeposit(50_000_000), '5000만');
  assert.equal(formatKoreanDeposit(100_000_000), '1억');
  assert.equal(formatKoreanDeposit(125_500_000), '1억 2550만');
  assert.equal(formatKoreanDeposit(0), '0');
});

test('설정한 비서에 해당하는 멘트 또는 이미지를 선택한다', () => {
  const values = {
    cat: 'cat-value',
    man: 'man-value',
    woman: 'woman-value',
  };

  assert.equal(selectSecretaryValue(values, 'cat'), 'cat-value');
  assert.equal(selectSecretaryValue(values, 'man'), 'man-value');
  assert.equal(selectSecretaryValue(values, 'woman'), 'woman-value');
});

test('알 수 없는 비서 설정은 고양이 설정으로 안전하게 대체한다', () => {
  const values = { cat: 'cat-value', man: 'man-value' };

  assert.equal(selectSecretaryValue(values, 'unknown'), 'cat-value');
});
