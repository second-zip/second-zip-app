-- =========================================================
-- Database
-- =========================================================

CREATE DATABASE IF NOT EXISTS secondzip
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE secondzip;


-- =========================================================
-- 기존 테이블 삭제
-- FK 의존성 때문에 자식 테이블부터 삭제
-- =========================================================

DROP TABLE IF EXISTS report_fraud_detail_results;
DROP TABLE IF EXISTS report_fraud_types;
DROP TABLE IF EXISTS report_check_results;
DROP TABLE IF EXISTS ai_generate_messages;
DROP TABLE IF EXISTS account_checklist_items;
DROP TABLE IF EXISTS checklist_items;
DROP TABLE IF EXISTS account_term_consents;
DROP TABLE IF EXISTS terms;
DROP TABLE IF EXISTS analysis_reports;
DROP TABLE IF EXISTS fraud_damage_statistics;
DROP TABLE IF EXISTS jeonse_price_indices;
DROP TABLE IF EXISTS regions;
DROP TABLE IF EXISTS accounts;


-- =========================================================
-- 1. 회원
-- =========================================================

CREATE TABLE accounts (
    account_id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    character_type ENUM(
        'DEFAULT',
        'CAREFUL',
        'FRIENDLY'
    ) NOT NULL DEFAULT 'DEFAULT',

    PRIMARY KEY (account_id),
    UNIQUE KEY uk_accounts_email (email),
    UNIQUE KEY uk_accounts_nickname (nickname)
) ENGINE = InnoDB;


-- =========================================================
-- 2. 약관
-- =========================================================

CREATE TABLE terms (
    term_id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    term_type ENUM(
        'SERVICE',
        'PRIVACY_POLICY',
        'MARKETING'
    ) NOT NULL,
    is_required BOOLEAN NOT NULL,
    version VARCHAR(30) NOT NULL,

    PRIMARY KEY (term_id),
    UNIQUE KEY uk_terms_type_version (term_type, version)
) ENGINE = InnoDB;


-- =========================================================
-- 3. 회원 약관 동의
-- =========================================================

CREATE TABLE account_term_consents (
    account_term_consent_id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    is_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    agreed_at DATETIME NULL,

    PRIMARY KEY (account_term_consent_id),

    -- 한 회원이 같은 약관에 대한 동의 기록을 중복 생성하는 것 방지
    UNIQUE KEY uk_account_term_consents_account_term (
        account_id,
        term_id
    ),

    KEY idx_account_term_consents_term_id (term_id),

    CONSTRAINT fk_account_term_consents_account
        FOREIGN KEY (account_id)
        REFERENCES accounts (account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_account_term_consents_term
        FOREIGN KEY (term_id)
        REFERENCES terms (term_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 4. 분석 리포트
-- =========================================================

CREATE TABLE analysis_reports (
    analysis_report_id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    road_address VARCHAR(255) NOT NULL,
    detail_address VARCHAR(255) NULL,
    deposit BIGINT NOT NULL,
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    favorited_at DATETIME NULL,
    share_token VARCHAR(255) NULL,
    share_expires_at DATETIME NULL,
    risk_level ENUM(
        'SAFE',
        'CAUTION',
        'DANGER'
    ) NULL,

    PRIMARY KEY (analysis_report_id),
    UNIQUE KEY uk_analysis_reports_share_token (share_token),
    KEY idx_analysis_reports_account_id (account_id),
    KEY idx_analysis_reports_favorite (
        account_id,
        favorite,
        favorited_at
    ),

    CONSTRAINT chk_analysis_reports_deposit
        CHECK (deposit >= 0),

    CONSTRAINT fk_analysis_reports_account
        FOREIGN KEY (account_id)
        REFERENCES accounts (account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 5. AI 생성 메시지
-- =========================================================

CREATE TABLE ai_generate_messages (
    ai_generate_message_id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_report_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    reason TEXT NULL,

    PRIMARY KEY (ai_generate_message_id),
    KEY idx_ai_generate_messages_report_id (analysis_report_id),

    CONSTRAINT fk_ai_generate_messages_report
        FOREIGN KEY (analysis_report_id)
        REFERENCES analysis_reports (analysis_report_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 6. 필수 점검 결과
-- =========================================================

CREATE TABLE report_check_results (
    report_check_result_id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_report_id BIGINT NOT NULL,
    check_type ENUM(
		'MORTGAGE_EXISTENCE',
		'ILLEGAL_BUILDING',
		'BUILDING_USE',
		'HUG_GUARANTEE_ELIGIBILITY',
		'RIGHTS_INFRINGEMENT'
    ) NOT NULL,
    risk_level ENUM(
        'SAFE',
        'CAUTION',
        'DANGER'
    ) NOT NULL,

    PRIMARY KEY (report_check_result_id),

    -- 하나의 리포트에서 동일 점검 항목 중복 방지
    UNIQUE KEY uk_report_check_results_report_type (
        analysis_report_id,
        check_type
    ),

    CONSTRAINT fk_report_check_results_report
        FOREIGN KEY (analysis_report_id)
        REFERENCES analysis_reports (analysis_report_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 7. 리포트별 전세사기 유형 결과
-- =========================================================

CREATE TABLE report_fraud_types (
    report_fraud_type_id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_report_id BIGINT NOT NULL,
    fraud_type ENUM(
		'UNDERWATER_JEONSE',
		'FALSE_INFORMATION_RIGHTS_CONCEALMENT',
		'TRUST_PROPERTY_FRAUD'
    ) NOT NULL,
    risk_level ENUM(
        'SAFE',
        'CAUTION',
        'DANGER'
    ) NOT NULL,

    PRIMARY KEY (report_fraud_type_id),

    -- 하나의 리포트에 같은 사기 유형 중복 저장 방지
    UNIQUE KEY uk_report_fraud_types_report_type (
        analysis_report_id,
        fraud_type
    ),

    CONSTRAINT fk_report_fraud_types_report
        FOREIGN KEY (analysis_report_id)
        REFERENCES analysis_reports (analysis_report_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 8. 전세사기 유형 상세 결과
-- =========================================================

CREATE TABLE report_fraud_detail_results (
    report_fraud_detail_result_id BIGINT NOT NULL AUTO_INCREMENT,
    report_fraud_type_id BIGINT NOT NULL,
    detail_type ENUM(
        'HIGH_JEONSE_RATIO',
        'PRIORITY_DEBT_BURDEN',
        'HUG_GUARANTEE_PRECHECK',

        'LAND_BUILDING_OWNERSHIP_MISMATCH',
        'FALSE_BUILDING_USE_INFORMATION',
        'RIGHTS_INFRINGEMENT_CONCEALMENT',

        'TRUST_REGISTRATION_EXISTENCE',
        'REGISTERED_OWNER_VERIFICATION',
        'POST_TRUST_RIGHTS_INFRINGEMENT'
    ) NOT NULL,
    risk_level ENUM(
        'SAFE',
        'CAUTION',
        'DANGER'
    ) NOT NULL,

    PRIMARY KEY (report_fraud_detail_result_id),
    UNIQUE KEY uk_fraud_detail_results_type (
        report_fraud_type_id,
        detail_type
    ),

    CONSTRAINT fk_fraud_type_detail_fraud_type
        FOREIGN KEY (report_fraud_type_id)
        REFERENCES report_fraud_types (report_fraud_type_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 9. 체크리스트 기본 항목
-- =========================================================

CREATE TABLE checklist_items (
    checklist_item_id BIGINT NOT NULL AUTO_INCREMENT,
    contents VARCHAR(500) NOT NULL,

    PRIMARY KEY (checklist_item_id)
) ENGINE = InnoDB;


-- =========================================================
-- 10. 회원별 체크리스트 상태
-- =========================================================

CREATE TABLE account_checklist_items (
    account_checklist_item_id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    checklist_item_id BIGINT NOT NULL,
    is_checked BOOLEAN NOT NULL DEFAULT FALSE,

    PRIMARY KEY (account_checklist_item_id),

    -- 같은 회원에게 같은 체크리스트가 중복 생성되는 것 방지
    UNIQUE KEY uk_account_checklist_items_account_item (
        account_id,
        checklist_item_id
    ),

    KEY idx_account_checklist_items_item_id (checklist_item_id),

    CONSTRAINT fk_account_checklist_items_account
        FOREIGN KEY (account_id)
        REFERENCES accounts (account_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_account_checklist_items_item
        FOREIGN KEY (checklist_item_id)
        REFERENCES checklist_items (checklist_item_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 11. 행정구역
-- =========================================================

CREATE TABLE regions (
    region_id BIGINT NOT NULL AUTO_INCREMENT,
    region_code VARCHAR(20) NOT NULL,
    region_name VARCHAR(100) NOT NULL,
    region_level ENUM(
        'SIDO',
        'SIGUNGU',
        'EUPMYEONDONG'
    ) NOT NULL,
    parent_region_id BIGINT NULL,

    PRIMARY KEY (region_id),
    UNIQUE KEY uk_regions_region_code (region_code),
    KEY idx_regions_parent_region_id (parent_region_id),
    KEY idx_regions_name_level (region_name, region_level),

    CONSTRAINT fk_regions_parent
        FOREIGN KEY (parent_region_id)
        REFERENCES regions (region_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 12. HUG 전세사기 피해주택 통계
-- =========================================================

CREATE TABLE fraud_damage_statistics (
    fraud_damage_statistic_id BIGINT NOT NULL AUTO_INCREMENT,
    region_id BIGINT NOT NULL,
    damage_house_count INT NOT NULL DEFAULT 0,
    base_date DATE NOT NULL,

    PRIMARY KEY (fraud_damage_statistic_id),

    -- 같은 지역과 기준일의 데이터 중복 방지
    UNIQUE KEY uk_fraud_damage_statistics_region_date (
        region_id,
        base_date
    ),

    CONSTRAINT chk_fraud_damage_statistics_count
        CHECK (damage_house_count >= 0),

    CONSTRAINT fk_fraud_damage_statistics_region
        FOREIGN KEY (region_id)
        REFERENCES regions (region_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 13. 한국부동산원 전세가격지수
-- =========================================================

CREATE TABLE jeonse_price_indices (
    jeonse_price_index_id BIGINT NOT NULL AUTO_INCREMENT,
    region_id BIGINT NOT NULL,
    base_month DATE NOT NULL,
    price_index DECIMAL(10, 4) NOT NULL,
    change_rate DECIMAL(10, 4) NULL,

    PRIMARY KEY (jeonse_price_index_id),

    -- 한 지역에 같은 기준 월 데이터 중복 방지
    UNIQUE KEY uk_jeonse_price_indices_region_month (
        region_id,
        base_month
    ),

    CONSTRAINT chk_jeonse_price_indices_price
        CHECK (price_index >= 0),

    CONSTRAINT fk_jeonse_price_indices_region
        FOREIGN KEY (region_id)
        REFERENCES regions (region_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;