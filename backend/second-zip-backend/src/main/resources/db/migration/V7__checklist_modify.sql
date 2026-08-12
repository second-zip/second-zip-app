-- =========================================================
-- 1. 체크리스트 설명 컬럼 추가
-- =========================================================
ALTER TABLE checklist_items
    ADD COLUMN description VARCHAR(500) NULL AFTER contents;

-- =========================================================
-- 1-1. 삭제할 체크리스트를 참조하고 있는 데이터 제거
--
-- COMMON 항목은 유지한다.
-- 각 주택 유형별
--   - 보증보험 가입 여부
--   - 국세 / 지방세 체납
--   - 잔금 전 등기부 재확인
-- 만 제거
-- =========================================================


-- 분석 결과에서 참조 제거
DELETE arcr
FROM analysis_report_checklist_results arcr
         INNER JOIN checklist_items ci
                    ON ci.checklist_item_id = arcr.checklist_item_id
WHERE ci.category <> 'COMMON'
  AND ci.contents IN (
                      '보증보험 가입 가능 여부',
                      '국세·지방세 체납',
                      '잔금 전 등기부 재확인'
    );


-- 실제 리포트 체크리스트에서 참조 제거
DELETE arci
FROM analysis_report_checklist_items arci
         INNER JOIN checklist_items ci
                    ON ci.checklist_item_id = arci.checklist_item_id
WHERE ci.category <> 'COMMON'
  AND ci.contents IN (
                      '보증보험 가입 가능 여부',
                      '국세·지방세 체납',
                      '잔금 전 등기부 재확인'
    );


-- 녹음 체크리스트 분석 결과에서 참조 제거
DELETE rcr
FROM recording_checklist_results rcr
         INNER JOIN checklist_items ci
                    ON ci.checklist_item_id = rcr.checklist_item_id
WHERE ci.category <> 'COMMON'
  AND ci.contents IN (
                      '보증보험 가입 가능 여부',
                      '국세·지방세 체납',
                      '잔금 전 등기부 재확인'
    );


-- =========================================================
-- 1-2. checklist_items에서 실제 항목 제거
-- =========================================================
DELETE FROM checklist_items
WHERE category <> 'COMMON'
  AND contents IN (
                   '보증보험 가입 가능 여부',
                   '국세·지방세 체납',
                   '잔금 전 등기부 재확인'
    );


-- =========================================================
-- 2. 공통 체크리스트 설명
-- =========================================================
UPDATE checklist_items
SET description = '근저당·압류·가압류·신탁등기 등 권리관계 확인합니다.'
WHERE contents = '등기부등본 확인'
  AND category = 'COMMON';

UPDATE checklist_items
SET description = '위반건축물·용도를 확인합니다.'
WHERE contents = '건축물대장 확인'
  AND category = 'COMMON';

UPDATE checklist_items
SET description = '시세 대비 보증금이 과도한지 확인합니다.'
WHERE contents = '전세가율 확인'
  AND category = 'COMMON';

UPDATE checklist_items
SET description = '가입 거절 시 위험 신호를 확인합니다.'
WHERE contents = 'HUG/HF/SGI 보증보험 가능 여부 확인'
  AND category = 'COMMON';

UPDATE checklist_items
SET description = '조세채권 우선변제로 보증금 회수 위험을 점검합니다.'
WHERE contents = '국세·지방세 체납 확인'
  AND category = 'COMMON';

UPDATE checklist_items
SET description = '계약 후 권리변동 여부를 확인합니다.'
WHERE contents = '잔금 지급 직전 등기부 재확인'
  AND category = 'COMMON';


-- =========================================================
-- 3. 다가구주택 체크리스트 설명
-- =========================================================
UPDATE checklist_items
SET description = '선순위 임차인 보증금이 많으면 내 보증금보다 먼저 배당되어 회수하지 못할 수 있습니다.'
WHERE contents = '선순위 임차인 보증금'
  AND category = 'MULTI_FAMILY';

UPDATE checklist_items
SET description = '선순위 임차인의 확정일자와 보증금을 확인합니다.'
WHERE contents = '확정일자 부여현황'
  AND category = 'MULTI_FAMILY';

UPDATE checklist_items
SET description = '전입신고만 한 대항력 있는 임차인이 있는지 확인합니다.'
WHERE contents = '전입세대확인서'
  AND category = 'MULTI_FAMILY';

UPDATE checklist_items
SET description = '체납 세금은 보증금보다 먼저 배당될 수 있습니다.'
WHERE contents = '국세·지방세 체납'
  AND category = 'MULTI_FAMILY';

UPDATE checklist_items
SET description = '계약 이후 근저당이나 압류가 새로 설정될 수 있습니다.'
WHERE contents = '잔금 전 등기부 재확인'
  AND category = 'MULTI_FAMILY';


-- =========================================================
-- 4. 단독주택 체크리스트 설명
-- =========================================================
UPDATE checklist_items
SET description = '건물과 토지 소유자가 다르면 권리관계가 복잡해질 수 있습니다.'
WHERE contents = '건물·토지 소유자 동일 여부'
  AND category = 'SINGLE_FAMILY';

UPDATE checklist_items
SET description = '위반건축물은 보증보험 가입이 제한될 수 있습니다.'
WHERE contents = '위반건축물 여부'
  AND category = 'SINGLE_FAMILY';

UPDATE checklist_items
SET description = '계약 권한이 없는 사람과 계약하면 분쟁이 발생할 수 있습니다.'
WHERE contents = '대리계약 여부'
  AND category = 'SINGLE_FAMILY';

UPDATE checklist_items
SET description = '조세채권이 우선 변제될 수 있습니다.'
WHERE contents = '국세·지방세 체납'
  AND category = 'SINGLE_FAMILY';

UPDATE checklist_items
SET description = '계약 이후 권리변동 여부를 확인합니다.'
WHERE contents = '잔금 전 등기부 재확인'
  AND category = 'SINGLE_FAMILY';


-- =========================================================
-- 5. 아파트 체크리스트 설명
-- =========================================================
UPDATE checklist_items
SET description = '근저당·압류·가압류가 있으면 보증금 회수 위험이 있습니다.'
WHERE contents = '권리관계 확인'
  AND category = 'APARTMENT';

UPDATE checklist_items
SET description = '시세 대비 보증금이 높으면 깡통전세 위험이 증가합니다.'
WHERE contents = '전세가율'
  AND category = 'APARTMENT';

UPDATE checklist_items
SET description = '가입이 거절되면 위험 신호일 수 있습니다.'
WHERE contents = '보증보험 가입 가능 여부'
  AND category = 'APARTMENT';

UPDATE checklist_items
SET description = '조세채권이 우선 배당될 수 있습니다.'
WHERE contents = '국세·지방세 체납'
  AND category = 'APARTMENT';

UPDATE checklist_items
SET description = '계약 이후 권리변동 여부를 확인합니다.'
WHERE contents = '잔금 전 등기부 재확인'
  AND category = 'APARTMENT';


-- =========================================================
-- 6. 다세대·연립주택 체크리스트 설명
-- =========================================================
UPDATE checklist_items
SET description = '다른 호실의 채무가 내 보증금 회수에 영향을 줄 수 있습니다.'
WHERE contents = '공동근저당'
  AND category = 'MULTI_HOUSEHOLD';

UPDATE checklist_items
SET description = '보증보험 가입이 제한될 수 있습니다.'
WHERE contents = '위반건축물'
  AND category = 'MULTI_HOUSEHOLD';

UPDATE checklist_items
SET description = '시세 대비 보증금이 높으면 위험합니다.'
WHERE contents = '전세가율'
  AND category = 'MULTI_HOUSEHOLD';

UPDATE checklist_items
SET description = '가입 거절 시 위험 신호입니다.'
WHERE contents = '보증보험 가입 가능 여부'
  AND category = 'MULTI_HOUSEHOLD';

UPDATE checklist_items
SET description = '조세채권 우선 변제 가능성이 있습니다.'
WHERE contents = '국세·지방세 체납'
  AND category = 'MULTI_HOUSEHOLD';

UPDATE checklist_items
SET description = '계약 이후 공동근저당 등이 추가될 수 있습니다.'
WHERE contents = '잔금 전 등기부 재확인'
  AND category = 'MULTI_HOUSEHOLD';


-- =========================================================
-- 7. 오피스텔 체크리스트 설명
-- =========================================================
UPDATE checklist_items
SET description = '업무용은 제도 적용과 보증보험 가입 조건이 달라질 수 있습니다.'
WHERE contents = '주거용/업무용 여부'
  AND category = 'OFFICETEL';

UPDATE checklist_items
SET description = '계약 권한이 신탁회사에 있을 수 있습니다.'
WHERE contents = '신탁등기 여부'
  AND category = 'OFFICETEL';

UPDATE checklist_items
SET description = '동의 없이 계약하면 분쟁이 발생할 수 있습니다.'
WHERE contents = '신탁회사 동의'
  AND category = 'OFFICETEL';

UPDATE checklist_items
SET description = '가입이 제한될 수 있습니다.'
WHERE contents = '보증보험 가입 가능 여부'
  AND category = 'OFFICETEL';

UPDATE checklist_items
SET description = '조세채권 우선 변제 가능성이 있습니다.'
WHERE contents = '국세·지방세 체납'
  AND category = 'OFFICETEL';

UPDATE checklist_items
SET description = '계약 이후 권리관계 변경 여부를 확인합니다.'
WHERE contents = '잔금 전 등기부 재확인'
  AND category = 'OFFICETEL';


-- =========================================================
-- 8. 신탁주택
-- V5에서 '신탁회사 동의'만 유지한 경우
-- =========================================================
UPDATE checklist_items
SET description = '동의가 필요한 경우 계약 효력에 영향을 줄 수 있습니다.'
WHERE contents = '신탁회사 동의'
  AND category = 'TRUST_PROPERTY';