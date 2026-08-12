-- =========================================================
-- report_check_results 복구
--
-- V5가 이 테이블을 DROP했으나 ReportMapper는 여전히 INSERT/SELECT 한다.
-- V5가 적용된 DB에서는 필수점검 5개 저장 단계에서 리포트 생성 전체가 실패한다.
--
-- V1 정의에 V3의 data_status 컬럼을 반영해 재생성한다.
-- 이미 삭제된 과거 데이터는 복구할 수 없다.
-- =========================================================

CREATE TABLE IF NOT EXISTS report_check_results
(
    report_check_result_id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_report_id     BIGINT NOT NULL,

    check_type             ENUM (
        'MORTGAGE_EXISTENCE',
        'ILLEGAL_BUILDING',
        'BUILDING_USE',
        'HUG_GUARANTEE_ELIGIBILITY',
        'RIGHTS_INFRINGEMENT'
        )                         NOT NULL,

    risk_level             ENUM (
        'SAFE',
        'CAUTION',
        'DANGER'
        )                         NOT NULL,

    -- V3에서 추가된 컬럼. 위험도와 데이터 확보 여부를 분리한다.
    data_status            ENUM (
        'VERIFIED',
        'UNVERIFIED',
        'NOT_APPLICABLE'
        )                         NOT NULL DEFAULT 'VERIFIED',

    evidence               JSON NULL,

    created_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

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
