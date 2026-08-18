-- =========================================================
-- 체크리스트 중복 항목 제거
--
-- 분석 결과 자동 체크(analysis_report_checklist_results)를 붙이면서
-- 드러난 문제. 체크리스트는 "COMMON + 주택유형(+ TRUST_PROPERTY)"으로
-- 조립되는데, 유형별 항목 중 일부가 COMMON과 같은 일을 가리키고 있었다.
--
-- 1) 완전히 같은 문구가 두 줄로 보이는 경우
--    오피스텔이면서 신탁주택이면
--    OFFICETEL/'신탁회사 동의' 와 TRUST_PROPERTY/'신탁회사 동의' 가
--    둘 다 조회된다. UNIQUE 키가 (category, contents) 라 DB는 막지 못한다.
--    '신탁회사 동의'는 신탁이 존재해야 의미가 있으므로
--    TRUST_PROPERTY 쪽만 남긴다.
--
-- 2) 같은 판정이 두 항목을 동시에 체크하는 경우
--    APARTMENT/'권리관계 확인'  = COMMON/'등기부등본 확인' (MORTGAGE+RIGHTS)
--    APARTMENT/'전세가율'       = COMMON/'전세가율 확인'   (HIGH_JEONSE_RATIO)
--    MULTI_HOUSEHOLD/'전세가율' = COMMON/'전세가율 확인'   (HIGH_JEONSE_RATIO)
--    사용자 화면에 "전세가율 확인"과 "전세가율"이 나란히 뜬다.
--    COMMON 은 모든 유형이 공유하므로 유형별 쪽을 지운다.
--
-- 결과: APARTMENT 는 유형별 고유 항목이 없어져 COMMON 6개만 남는다.
--       아파트는 공통 점검 외에 별도 위험 항목이 없다는 판단.
--
-- 삭제 순서는 V7 과 동일한 패턴을 따른다.
-- checklist_items 를 참조하는 4개 테이블 중
--   - account_checklist_items        : ON DELETE CASCADE (자동 정리)
--   - analysis_report_checklist_items   : 기본 RESTRICT → 먼저 지운다
--   - analysis_report_checklist_results : 기본 RESTRICT → 먼저 지운다
--   - recording_checklist_results       : 기본 RESTRICT → 먼저 지운다
--
-- 기존 체크리스트에서 해당 항목의 체크 상태는 함께 사라진다.
-- 중복 항목이라 같은 내용의 COMMON 항목이 그대로 남으므로 정보 손실은 없다.
-- =========================================================


-- 삭제 대상을 한 번만 정의해 두고 세 테이블에서 동일하게 참조한다.
--   (OFFICETEL,        '신탁회사 동의')
--   (APARTMENT,        '권리관계 확인')
--   (APARTMENT,        '전세가율')
--   (MULTI_HOUSEHOLD,  '전세가율')


-- =========================================================
-- 1. 분석 자동 체크 결과에서 참조 제거
-- =========================================================
DELETE arcr
FROM analysis_report_checklist_results arcr
         INNER JOIN checklist_items ci
                    ON ci.checklist_item_id = arcr.checklist_item_id
WHERE (ci.category = 'OFFICETEL'       AND ci.contents = '신탁회사 동의')
   OR (ci.category = 'APARTMENT'       AND ci.contents = '권리관계 확인')
   OR (ci.category = 'APARTMENT'       AND ci.contents = '전세가율')
   OR (ci.category = 'MULTI_HOUSEHOLD' AND ci.contents = '전세가율');


-- =========================================================
-- 2. 실제 리포트 체크리스트에서 참조 제거
-- =========================================================
DELETE arci
FROM analysis_report_checklist_items arci
         INNER JOIN checklist_items ci
                    ON ci.checklist_item_id = arci.checklist_item_id
WHERE (ci.category = 'OFFICETEL'       AND ci.contents = '신탁회사 동의')
   OR (ci.category = 'APARTMENT'       AND ci.contents = '권리관계 확인')
   OR (ci.category = 'APARTMENT'       AND ci.contents = '전세가율')
   OR (ci.category = 'MULTI_HOUSEHOLD' AND ci.contents = '전세가율');


-- =========================================================
-- 3. 녹음 체크리스트 분석 결과에서 참조 제거
-- =========================================================
DELETE rcr
FROM recording_checklist_results rcr
         INNER JOIN checklist_items ci
                    ON ci.checklist_item_id = rcr.checklist_item_id
WHERE (ci.category = 'OFFICETEL'       AND ci.contents = '신탁회사 동의')
   OR (ci.category = 'APARTMENT'       AND ci.contents = '권리관계 확인')
   OR (ci.category = 'APARTMENT'       AND ci.contents = '전세가율')
   OR (ci.category = 'MULTI_HOUSEHOLD' AND ci.contents = '전세가율');


-- =========================================================
-- 4. checklist_items 에서 실제 항목 제거
--    (account_checklist_items 는 ON DELETE CASCADE 로 함께 정리된다)
-- =========================================================
DELETE FROM checklist_items
WHERE (category = 'OFFICETEL'       AND contents = '신탁회사 동의')
   OR (category = 'APARTMENT'       AND contents = '권리관계 확인')
   OR (category = 'APARTMENT'       AND contents = '전세가율')
   OR (category = 'MULTI_HOUSEHOLD' AND contents = '전세가율');
