-- =========================================================
-- 1. 분석 리포트에 자동 판별된 주택 유형 추가
-- =========================================================
ALTER TABLE analysis_reports
    ADD COLUMN housing_category VARCHAR(50) NULL;


-- =========================================================
-- 2. 리포트 분석 단계에서 이미 검증된 체크리스트 항목
-- =========================================================
CREATE TABLE analysis_report_checklist_results (
                                                   analysis_report_checklist_result_id BIGINT NOT NULL AUTO_INCREMENT,
                                                   analysis_report_id BIGINT NOT NULL,
                                                   checklist_item_id BIGINT NOT NULL,
                                                   is_verified BOOLEAN NOT NULL DEFAULT FALSE,

                                                   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                   modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                       ON UPDATE CURRENT_TIMESTAMP,

                                                   PRIMARY KEY (analysis_report_checklist_result_id),

                                                   CONSTRAINT uk_analysis_report_checklist_result
                                                       UNIQUE (analysis_report_id, checklist_item_id),

                                                   CONSTRAINT fk_analysis_report_checklist_result_report
                                                       FOREIGN KEY (analysis_report_id)
                                                           REFERENCES analysis_reports(analysis_report_id)
                                                           ON DELETE CASCADE,

                                                   CONSTRAINT fk_analysis_report_checklist_result_item
                                                       FOREIGN KEY (checklist_item_id)
                                                           REFERENCES checklist_items(checklist_item_id)
);


-- =========================================================
-- 3. 리포트 전용 체크리스트
-- Report 1 : 0..1 Checklist
-- =========================================================
CREATE TABLE report_checklists (
                                   report_checklist_id BIGINT NOT NULL AUTO_INCREMENT,
                                   analysis_report_id BIGINT NOT NULL,
                                   account_id BIGINT NOT NULL,

                                   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,

                                   PRIMARY KEY (report_checklist_id),

                                   CONSTRAINT uk_report_checklist_report
                                       UNIQUE (analysis_report_id),

                                   CONSTRAINT fk_report_checklist_report
                                       FOREIGN KEY (analysis_report_id)
                                           REFERENCES analysis_reports(analysis_report_id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_report_checklist_account
                                       FOREIGN KEY (account_id)
                                           REFERENCES accounts(account_id)
                                           ON DELETE CASCADE
);


-- =========================================================
-- 4. 실제 리포트 체크리스트 항목
-- =========================================================
CREATE TABLE report_checklist_items (
                                        report_checklist_item_id BIGINT NOT NULL AUTO_INCREMENT,
                                        report_checklist_id BIGINT NOT NULL,
                                        checklist_item_id BIGINT NOT NULL,
                                        is_checked BOOLEAN NOT NULL DEFAULT FALSE,

                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,

                                        PRIMARY KEY (report_checklist_item_id),

                                        CONSTRAINT uk_report_checklist_item
                                            UNIQUE (report_checklist_id, checklist_item_id),

                                        CONSTRAINT fk_report_checklist_item_checklist
                                            FOREIGN KEY (report_checklist_id)
                                                REFERENCES report_checklists(report_checklist_id)
                                                ON DELETE CASCADE,

                                        CONSTRAINT fk_report_checklist_item_master
                                            FOREIGN KEY (checklist_item_id)
                                                REFERENCES checklist_items(checklist_item_id)
);


-- =========================================================
-- 5. 녹음 세션
-- =========================================================
CREATE TABLE recording_sessions (
                                    recording_session_id BIGINT NOT NULL AUTO_INCREMENT,
                                    account_id BIGINT NOT NULL,
                                    report_checklist_id BIGINT NULL,

                                    original_file_name VARCHAR(255) NULL,
                                    storage_object_key VARCHAR(500) NULL,
                                    content_type VARCHAR(100) NULL,
                                    file_size BIGINT NULL,

                                    status VARCHAR(30) NOT NULL,
                                    full_transcript LONGTEXT NULL,
                                    summary TEXT NULL,
                                    failure_reason VARCHAR(1000) NULL,
                                    started_at DATETIME NULL,
                                    ended_at DATETIME NULL,

                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,

                                    PRIMARY KEY (recording_session_id),

                                    CONSTRAINT fk_recording_session_account
                                        FOREIGN KEY (account_id)
                                            REFERENCES accounts(account_id),

                                    CONSTRAINT fk_recording_session_report_checklist
                                        FOREIGN KEY (report_checklist_id)
                                            REFERENCES report_checklists(report_checklist_id)
                                            ON DELETE CASCADE
);


-- =========================================================
-- 6. 녹음 기반 체크리스트 분석 결과
-- =========================================================
CREATE TABLE recording_checklist_results (
                                             recording_checklist_result_id BIGINT NOT NULL AUTO_INCREMENT,
                                             recording_session_id BIGINT NOT NULL,
                                             checklist_item_id BIGINT NOT NULL,

                                             status VARCHAR(30) NOT NULL DEFAULT 'UNCHECKED',
                                             confidence_score DECIMAL(5,4) NULL,
                                             evidence_text TEXT NULL,
                                             reason TEXT NULL,

                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                 ON UPDATE CURRENT_TIMESTAMP,

                                             PRIMARY KEY (recording_checklist_result_id),

                                             CONSTRAINT uk_recording_checklist_result
                                                 UNIQUE (recording_session_id, checklist_item_id),

                                             CONSTRAINT fk_recording_result_session
                                                 FOREIGN KEY (recording_session_id)
                                                     REFERENCES recording_sessions(recording_session_id),

                                             CONSTRAINT fk_recording_result_item
                                                 FOREIGN KEY (checklist_item_id)
                                                     REFERENCES checklist_items(checklist_item_id)
);