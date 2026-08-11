package com.secondzip.backend.record.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordingAsyncService {

    private final RecordingTranscriptionService recordingTranscriptionService;

    @Async
    public void transcribe(
            Long recordingSessionId,
            String storageObjectKey
    ) {
        recordingTranscriptionService.transcribe(
                recordingSessionId,
                storageObjectKey
        );
    }
}