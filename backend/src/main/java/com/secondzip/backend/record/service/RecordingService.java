package com.secondzip.backend.record.service;

import com.secondzip.backend.record.dto.response.RecordingLiveStartResponseDTO;
import com.secondzip.backend.record.dto.response.RecordingSessionResponseDTO;
import com.secondzip.backend.record.dto.response.RecordingStatusResponseDTO;
import org.springframework.web.multipart.MultipartFile;

//녹음 기능 진입. 파일 녹음 세션 생성, STT 시작, 상태 조회, 실시간 세션 생성 API 비즈니스 로직
public interface RecordingService {
    RecordingSessionResponseDTO createSession(
            Long accountId,
            Long reportChecklistId,
            MultipartFile file
    );

    void startTranscription(
            Long accountId,
            Long recordingSessionId
    );

    RecordingStatusResponseDTO getRecordingStatus(
            Long accountId,
            Long recordingSessionId
    );
    RecordingLiveStartResponseDTO startLiveRecording(Long accountId,Long reportChecklistId);

    void stopLiveRecording(
            Long accountId,
            Long recordingSessionId
    );
}
