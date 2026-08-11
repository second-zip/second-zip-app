-- =========================================================
-- 신탁주택 체크리스트 정리
-- '신탁회사 동의' 항목만 유지
-- =========================================================

DELETE FROM checklist_items
WHERE category = 'TRUST_PROPERTY'
  AND contents <> '신탁회사 동의';

DROP TABLE report_check_results;