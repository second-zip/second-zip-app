CREATE TABLE recording_sessions (
                                    recording_session_id BIGINT NOT NULL AUTO_INCREMENT,
                                    account_id BIGINT NOT NULL,
                                    audio_file_url VARCHAR(1000) NOT NULL,
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
                                            REFERENCES accounts(account_id)
);
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