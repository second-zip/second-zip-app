-- =========================================================
-- 위험도 판정에 실제로 쓰인 가격 정보(실거래가/공시가격)를
-- 리포트 최상위에서 바로 확인할 수 있게 저장한다.
--
-- 이전에는 HUG 보증 적격성 필수점검(HUG_GUARANTEE_ELIGIBILITY)의
-- evidence.basePrice에만 일부 반영돼 있어, 그 체크와 무관하게
-- 실거래가 자체를 확인할 방법이 없었다.
-- =========================================================
ALTER TABLE analysis_reports
    ADD COLUMN recent_sale_price BIGINT NULL COMMENT '최근 매매가(실거래가). 매칭된 거래가 없으면 NULL',
    ADD COLUMN official_price BIGINT NULL COMMENT '공시가격(환산 전 원본). 확인 못하면 NULL',
    ADD COLUMN base_price_source VARCHAR(50) NULL COMMENT '위험도 판정에 쓴 기준가 출처: RECENT_SALE_PRICE / OFFICIAL_PRICE_CONVERTED / NULL';
