// 분석 화면의 금액·비서·위험도 집계 로직을 검증하는 자동 테스트 파일입니다.
import assert from 'node:assert/strict';
import test from 'node:test';

import {
  formatKoreanDeposit,
  aggregateRiskStatuses,
  getAggregateRiskStatus,
  selectSecretaryValue,
} from './analysisLogic.js';

test('보증금을 억·만원 단위 한글 금액으로 표시한다', () => {
  assert.equal(formatKoreanDeposit(150_000_000), '1억 5000만원');
  assert.equal(formatKoreanDeposit(220_000_000), '2억 2000만원');
  assert.equal(formatKoreanDeposit(50_000_000), '0억 5000만원');
  assert.equal(formatKoreanDeposit(100_000_000), '1억 0만원');
  assert.equal(formatKoreanDeposit(125_500_000), '1억 2550만원');
  assert.equal(formatKoreanDeposit(150_000), '0억 15만원');
  assert.equal(formatKoreanDeposit(0), '0억 0만원');
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

test('세부 판정의 위험·주의 개수로 대표 상태를 선택한다', () => {
  assert.equal(
    getAggregateRiskStatus([{ status: 'safe' }, { status: 'danger' }]),
    'danger',
  );
  assert.equal(
    getAggregateRiskStatus([{ status: 'safe' }, { status: 'caution' }]),
    'caution',
  );
  assert.equal(
    getAggregateRiskStatus([
      { status: 'caution' },
      { status: 'caution' },
      { status: 'caution' },
    ]),
    'danger',
  );
  assert.equal(getAggregateRiskStatus([{ status: 'safe' }]), 'safe');
});

test('전체 판정도 위험 우선 및 주의 3개 이상 규칙을 적용한다', () => {
  assert.equal(aggregateRiskStatuses(['safe', 'safe']), 'safe');
  assert.equal(aggregateRiskStatuses(['safe', 'caution']), 'caution');
  assert.equal(
    aggregateRiskStatuses(['caution', 'caution', 'caution']),
    'danger',
  );
  assert.equal(aggregateRiskStatuses(['safe', 'danger']), 'danger');
});
