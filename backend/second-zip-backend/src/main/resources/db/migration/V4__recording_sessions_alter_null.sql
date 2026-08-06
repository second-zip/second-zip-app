ALTER TABLE recording_sessions
    MODIFY original_file_name VARCHAR(255) NULL,
    MODIFY storage_object_key VARCHAR(500) NULL,
    MODIFY content_type VARCHAR(100) NULL,
    MODIFY file_size BIGINT NULL;