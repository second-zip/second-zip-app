package com.secondzip.backend.report.record.service;

import com.secondzip.backend.record.service.RecordingAsyncService;
import com.secondzip.backend.record.service.RecordingTranscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecordingAsyncServiceTest {

    @Mock
    private RecordingTranscriptionService recordingTranscriptionService;

    @InjectMocks
    private RecordingAsyncService recordingAsyncService;

    @Test
    void transcribe_delegatesToTranscriptionService() {
        // given
        Long recordingSessionId = 10L;
        String objectKey = "input/1/test.mp3";

        // when
        recordingAsyncService.transcribe(
                recordingSessionId,
                objectKey
        );

        // then
        verify(recordingTranscriptionService)
                .transcribe(
                        recordingSessionId,
                        objectKey
                );
    }
}