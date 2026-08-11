-- =========================================================
-- 판정에 쓰인 데이터 상태를 위험도와 분리해 저장한다.
--
-- 기존에는 "위험해서 CAUTION"과 "데이터를 못 구해서 CAUTION"이 구분되지 않아,
-- 외부 API가 실패한 리포트가 사용자에게 그냥 "위험"으로 보였다.
-- risk_level ENUM은 건드리지 않고 별도 컬럼을 둔다.
--
-- VERIFIED       : 데이터를 확보해 실제로 판정함
-- UNVERIFIED     : 데이터를 확보하지 못해 판정 불가
-- NOT_APPLICABLE : 이 매물에 해당하지 않는 항목 (예: 집합건물의 토지 소유자 대조)
--
-- 기존 행은 판정 근거를 되살릴 수 없으므로 VERIFIED로 채운다.
-- =========================================================

ALTER TABLE report_check_results
    ADD COLUMN data_status ENUM(
        'VERIFIED',
        'UNVERIFIED',
        'NOT_APPLICABLE'
        ) NOT NULL DEFAULT 'VERIFIED' AFTER risk_level;

ALTER TABLE report_fraud_detail_results
    ADD COLUMN data_status ENUM(
        'VERIFIED',
        'UNVERIFIED',
        'NOT_APPLICABLE'
        ) NOT NULL DEFAULT 'VERIFIED' AFTER risk_level;
