-- =========================================================
-- 개발용 목업 데이터 (로컬 전용, external.api.mode=mock)
-- 실행: docker exec -i secondzip-mysql mysql -uroot -proot secondzip < 이파일
-- =========================================================

USE secondzip;

DROP TABLE IF EXISTS test_registry_data;
DROP TABLE IF EXISTS test_building_data;
DROP TABLE IF EXISTS test_price_data;

-- 등기부등본 목업
CREATE TABLE test_registry_data (
                                    test_registry_data_id BIGINT NOT NULL AUTO_INCREMENT,
                                    road_address VARCHAR(200) NOT NULL,
                                    mortgage_amount BIGINT NOT NULL DEFAULT 0,
                                    has_seizure BOOLEAN NOT NULL DEFAULT FALSE,
                                    has_trust_registration BOOLEAN NOT NULL DEFAULT FALSE,
                                    owner_name VARCHAR(50) NOT NULL,
                                    owner_type ENUM('INDIVIDUAL', 'TRUST_COMPANY', 'CORPORATION') NOT NULL DEFAULT 'INDIVIDUAL',
                                    land_owner_name VARCHAR(50) NOT NULL,
                                    has_post_trust_infringement BOOLEAN NOT NULL DEFAULT FALSE,
                                    PRIMARY KEY (test_registry_data_id),
                                    UNIQUE KEY uk_test_registry_address (road_address)
) ENGINE = InnoDB;

-- 건축물대장 목업
CREATE TABLE test_building_data (
                                    test_building_data_id BIGINT NOT NULL AUTO_INCREMENT,
                                    road_address VARCHAR(200) NOT NULL,
                                    is_illegal_building BOOLEAN NOT NULL DEFAULT FALSE,
                                    building_use VARCHAR(50) NOT NULL,
                                    building_type ENUM('SINGLE_FAMILY', 'MULTI_FAMILY', 'APARTMENT', 'MULTI_HOUSEHOLD', 'OFFICETEL') NOT NULL,
                                    PRIMARY KEY (test_building_data_id),
                                    UNIQUE KEY uk_test_building_address (road_address)
) ENGINE = InnoDB;

-- 실거래가/시세 목업
CREATE TABLE test_price_data (
                                 test_price_data_id BIGINT NOT NULL AUTO_INCREMENT,
                                 road_address VARCHAR(200) NOT NULL,
                                 recent_sale_price BIGINT NULL,
                                 official_price BIGINT NULL,
                                 PRIMARY KEY (test_price_data_id),
                                 UNIQUE KEY uk_test_price_address (road_address)
) ENGINE = InnoDB;

-- =========================================================
-- 시나리오 A: 전부 정상 (deposit 1억 입력 시 → 전체 SAFE)
-- =========================================================
INSERT INTO test_registry_data (road_address, mortgage_amount, has_seizure, has_trust_registration, owner_name, owner_type, land_owner_name, has_post_trust_infringement)
VALUES ('서울특별시 강남구 테헤란로 1', 0, FALSE, FALSE, '김철수', 'INDIVIDUAL', '김철수', FALSE);
INSERT INTO test_building_data (road_address, is_illegal_building, building_use, building_type)
VALUES ('서울특별시 강남구 테헤란로 1', FALSE, '공동주택', 'APARTMENT');
INSERT INTO test_price_data (road_address, recent_sale_price, official_price)
VALUES ('서울특별시 강남구 테헤란로 1', 200000000, 180000000);

-- =========================================================
-- 시나리오 B: 깡통전세 (deposit 1.7억 입력 시 → 전세가율 85% DANGER)
-- =========================================================
INSERT INTO test_registry_data (road_address, mortgage_amount, has_seizure, has_trust_registration, owner_name, owner_type, land_owner_name, has_post_trust_infringement)
VALUES ('서울특별시 마포구 월드컵로 22', 0, FALSE, FALSE, '이영희', 'INDIVIDUAL', '이영희', FALSE);
INSERT INTO test_building_data (road_address, is_illegal_building, building_use, building_type)
VALUES ('서울특별시 마포구 월드컵로 22', FALSE, '공동주택', 'MULTI_HOUSEHOLD');
INSERT INTO test_price_data (road_address, recent_sale_price, official_price)
VALUES ('서울특별시 마포구 월드컵로 22', 200000000, 170000000);

-- =========================================================
-- 시나리오 C: 신탁사기 (deposit 1억 → TRUST_PROPERTY DANGER)
-- =========================================================
INSERT INTO test_registry_data (road_address, mortgage_amount, has_seizure, has_trust_registration, owner_name, owner_type, land_owner_name, has_post_trust_infringement)
VALUES ('경기도 수원시 팔달구 정조로 33', 0, FALSE, TRUE, 'OO자산신탁', 'TRUST_COMPANY', 'OO자산신탁', TRUE);
INSERT INTO test_building_data (road_address, is_illegal_building, building_use, building_type)
VALUES ('경기도 수원시 팔달구 정조로 33', FALSE, '공동주택', 'OFFICETEL');
INSERT INTO test_price_data (road_address, recent_sale_price, official_price)
VALUES ('경기도 수원시 팔달구 정조로 33', 250000000, 220000000);

-- =========================================================
-- 시나리오 D: 복합위험 (deposit 0.8억 → 근저당+위반건축물+다가구HUG)
-- 선순위: (1.2억 + 0.8억) / 2.5억 = 80% > 54% → DANGER
-- =========================================================
INSERT INTO test_registry_data (road_address, mortgage_amount, has_seizure, has_trust_registration, owner_name, owner_type, land_owner_name, has_post_trust_infringement)
VALUES ('인천광역시 부평구 부평대로 44', 120000000, FALSE, FALSE, '박민수', 'INDIVIDUAL', '박민수', FALSE);
INSERT INTO test_building_data (road_address, is_illegal_building, building_use, building_type)
VALUES ('인천광역시 부평구 부평대로 44', TRUE, '공동주택', 'MULTI_FAMILY');
INSERT INTO test_price_data (road_address, recent_sale_price, official_price)
VALUES ('인천광역시 부평구 부평대로 44', 250000000, 200000000);

-- =========================================================
-- 시나리오 E: 확인불가+압류+소유불일치+근생 (deposit 1억)
-- 가격 NULL → 전세가율 CAUTION, 건물주(최지훈)≠토지주(정수아)
-- =========================================================
INSERT INTO test_registry_data (road_address, mortgage_amount, has_seizure, has_trust_registration, owner_name, owner_type, land_owner_name, has_post_trust_infringement)
VALUES ('부산광역시 해운대구 센텀로 55', 0, TRUE, FALSE, '최지훈', 'INDIVIDUAL', '정수아', FALSE);
INSERT INTO test_building_data (road_address, is_illegal_building, building_use, building_type)
VALUES ('부산광역시 해운대구 센텀로 55', FALSE, '근린생활시설', 'OFFICETEL');
INSERT INTO test_price_data (road_address, recent_sale_price, official_price)
VALUES ('부산광역시 해운대구 센텀로 55', NULL, NULL);