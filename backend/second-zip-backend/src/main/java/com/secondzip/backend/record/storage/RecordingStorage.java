package com.secondzip.backend.record.storage;

import org.springframework.web.multipart.MultipartFile;

public interface RecordingStorage {

    String upload(Long accountId, MultipartFile file);
}