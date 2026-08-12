ALTER TABLE analysis_reports
    ADD COLUMN request_id VARCHAR(36) NULL AFTER account_id,
    ADD CONSTRAINT uk_analysis_reports_request_id UNIQUE (request_id);
