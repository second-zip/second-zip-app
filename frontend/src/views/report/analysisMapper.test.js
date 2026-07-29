// 분석 API·미리보기 응답이 화면 데이터로 정확히 변환되는지 검증하는 파일입니다.
import assert from 'node:assert/strict';
import test from 'node:test';

import {
  mapCheckResults,
  mapFraudTypes,
  mapReportDetail,
  mapSecretary,
  mapSpecialTerms,
  toUiRisk,
} from './analysisMapper.js';
import {
  ANALYSIS_PREVIEW_REPORTS,
  MOCK_REPORT_DETAIL,
  SCENARIO_B_REPORT_DETAIL,
} from './analysisMock.js';

test('백엔드 위험도 enum을 화면 상태값으로 변환한다', () => {
  assert.equal(toUiRisk('SAFE'), 'safe');
  assert.equal(toUiRisk('CAUTION'), 'caution');
  assert.equal(toUiRisk('DANGER'), 'danger');
  assert.equal(toUiRisk('UNKNOWN'), 'caution');
  assert.equal(toUiRisk(null), 'caution');
});

test('필수 점검 응답을 화면 데이터로 변환한다', () => {
  const [check] = mapCheckResults([
    {
      checkType: 'MORTGAGE_EXISTENCE',
      result: 'SAFE',
      evidence: { mortgageAmount: 50_000_000 },
    },
  ]);

  assert.equal(check.id, 'mortgage');
  assert.equal(check.status, 'safe');
  assert.equal(check.amount, '0억 5000만원');
});

test('전세사기 유형 응답을 A/B/C 판정 데이터로 변환한다', () => {
  const [fraudType] = mapFraudTypes([
    {
      fraudType: 'UNDERWATER_JEONSE',
      riskLevel: 'CAUTION',
      detailResults: [{ detailType: 'HIGH_JEONSE_RATIO', result: 'DANGER' }],
    },
  ]);

  assert.equal(fraudType.id, 'gap-investment');
  assert.equal(fraudType.status, 'caution');
  assert.equal(fraudType.items[0].label, 'A. 높은 전세가율');
  assert.equal(fraudType.items[0].status, 'danger');
});

test('리포트 상세 응답의 기본 필드를 화면 상태로 묶는다', () => {
  const report = mapReportDetail({
    analysisReportId: 12,
    roadAddress: '서울시 마포구',
    detailAddress: '101동',
    deposit: 100_000_000,
    result: 'SAFE',
    favorite: true,
    checkResults: [],
    fraudTypes: [],
  });

  assert.equal(report.address, '서울시 마포구 101동');
  assert.equal(report.deposit, '100000000');
  assert.equal(report.risk, 'safe');
  assert.equal(report.favorite, true);
});

test('전체 mock 응답에서 필수 점검 5개와 사기 세부 판정 9개를 변환한다', () => {
  const report = mapReportDetail(MOCK_REPORT_DETAIL);
  const detailCount = report.fraudTypes.reduce(
    (count, fraudType) => count + fraudType.items.length,
    0,
  );

  assert.equal(report.checks.length, 5);
  assert.equal(report.fraudTypes.length, 3);
  assert.equal(detailCount, 9);
  assert.equal(report.risk, toUiRisk(MOCK_REPORT_DETAIL.result));
});

test('시나리오 B 응답을 높은 전세가율 위험 결과로 변환한다', () => {
  const report = mapReportDetail(SCENARIO_B_REPORT_DETAIL);
  const gapInvestment = report.fraudTypes.find(
    ({ id }) => id === 'gap-investment',
  );
  const highJeonseRatio = gapInvestment.items.find(({ label }) =>
    label.includes('높은 전세가율'),
  );

  assert.equal(report.address, '서울특별시 마포구 월드컵로 22 101호');
  assert.equal(report.deposit, '170000000');
  assert.equal(report.risk, 'danger');
  assert.equal(gapInvestment.status, 'danger');
  assert.equal(highJeonseRatio.status, 'danger');
});

test('시나리오 C부터 F까지 공통 분석 화면 데이터로 변환한다', () => {
  const expectedAddresses = {
    c: '경기도 수원시 팔달구 정조로 33 101호',
    d: '인천광역시 부평구 부평대로 44 101호',
    e: '부산광역시 해운대구 센텀로 55 101호',
    f: '서울특별시 서초구 검증로 66 101호',
  };

  Object.entries(expectedAddresses).forEach(([scenario, expectedAddress]) => {
    const report = mapReportDetail(ANALYSIS_PREVIEW_REPORTS[scenario]);

    assert.equal(report.address, expectedAddress);
    assert.equal(report.risk, 'danger');
    assert.equal(report.checks.length, 5);
    assert.equal(report.fraudTypes.length, 3);
  });
});

test('null 근거값은 하이픈으로, null 판정은 주의로 변환한다', () => {
  const report = mapReportDetail({
    roadAddress: null,
    detailAddress: null,
    deposit: 100_000_000,
    result: null,
    checkResults: [
      {
        checkType: 'MORTGAGE_EXISTENCE',
        result: null,
        evidence: { mortgageAmount: null },
      },
      {
        checkType: 'RIGHTS_INFRINGEMENT',
        result: null,
        evidence: { hasSeizure: null },
      },
    ],
    fraudTypes: [],
  });

  assert.equal(report.address, '-');
  assert.equal(report.risk, 'caution');
  assert.equal(report.checks[0].status, 'caution');
  assert.equal(report.checks[0].amount, '-');
  assert.equal(report.checks[1].status, 'caution');
  assert.equal(report.checks[1].amount, '-');
});

test('AI 특약 응답과 F의 6000자 설명을 화면 구조로 변환한다', () => {
  const terms = mapSpecialTerms(
    ANALYSIS_PREVIEW_REPORTS.f.aiSpecialTerms,
  );

  assert.equal(terms.length, 1);
  assert.equal(terms[0].description.length, 6000);
});

test('시나리오별 비서 설정을 화면 캐릭터 타입으로 변환한다', () => {
  assert.equal(mapSecretary(ANALYSIS_PREVIEW_REPORTS.b.secretary), 'man');
  assert.equal(mapSecretary(ANALYSIS_PREVIEW_REPORTS.c.secretary), 'cat');
  assert.equal(mapSecretary(ANALYSIS_PREVIEW_REPORTS.d.secretary), 'woman');
  assert.equal(mapSecretary('UNKNOWN'), null);
});
