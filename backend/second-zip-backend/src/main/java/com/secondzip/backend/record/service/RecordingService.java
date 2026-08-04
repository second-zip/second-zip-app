package com.secondzip.backend.record.service;

import com.secondzip.backend.record.dto.response.RecordingSessionResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface RecordingService {
    RecordingSessionResponseDTO createSession(Long accountId, MultipartFile file);
    void startTranscription(Long accountId, Long recordingSessionId,String category);
}
