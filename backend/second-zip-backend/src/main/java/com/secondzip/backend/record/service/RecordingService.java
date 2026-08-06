package com.secondzip.backend.record.service;

import com.secondzip.backend.record.dto.response.RecordingLiveStartResponseDTO;
import com.secondzip.backend.record.dto.response.RecordingSessionResponseDTO;
import com.secondzip.backend.record.dto.response.RecordingStatusResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface RecordingService {
    RecordingSessionResponseDTO createSession(Long accountId, MultipartFile file);
    void startTranscription(Long accountId, Long recordingSessionId,String category);
    RecordingStatusResponseDTO getRecordingStatus(
            Long accountId,
            Long recordingSessionId
    );
    RecordingLiveStartResponseDTO startLiveRecording(Long accountId);
}
