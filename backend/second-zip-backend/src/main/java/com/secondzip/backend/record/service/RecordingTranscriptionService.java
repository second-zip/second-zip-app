package com.secondzip.backend.record.service;

import com.secondzip.backend.record.client.SpeechToTextClient;
import com.secondzip.backend.record.enums.RecordingStatus;
import com.secondzip.backend.record.mapper.RecordingSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordingTranscriptionService {

    private final RecordingSessionMapper recordingSessionMapper;
    private final SpeechToTextClient speechToTextClient;

    public void transcribe(
            Long recordingSessionId,
            String storageObjectKey
    ) {
        try {
            recordingSessionMapper.updateStatus(
                    recordingSessionId,
                    RecordingStatus.TRANSCRIBING
            );

            String transcript = speechToTextClient.transcribe(storageObjectKey);

            recordingSessionMapper.updateTranscript(
                    recordingSessionId,
                    transcript,
                    RecordingStatus.COMPLETED
            );
        } catch (Exception e) {
            recordingSessionMapper.markFailed(
                    recordingSessionId,
                    safeFailureReason(e)
            );
        }
    }

    private String safeFailureReason(Exception e) {
        String message = e.getMessage();

        if (message == null || message.isBlank()) {
            return "음성 인식 처리 중 오류가 발생했습니다.";
        }

        return message.length() > 1000
                ? message.substring(0, 1000)
                : message;
    }
}