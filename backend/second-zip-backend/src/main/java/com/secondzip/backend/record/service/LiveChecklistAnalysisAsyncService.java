package com.secondzip.backend.record.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LiveChecklistAnalysisAsyncService {

    private final LiveChecklistAnalysisService liveChecklistAnalysisService;
    private final LiveRecordingContextManager contextManager;

    @Async
    public void analyze(
            Long recordingSessionId,
            String transcript,
            String category
    ) {
        try {
            liveChecklistAnalysisService.analyzeProvisional(
                    recordingSessionId,
                    transcript,
                    category
            );
        } finally {
            LiveRecordingContext context =
                    contextManager.get(recordingSessionId);

            context.finishAnalysis();
        }
    }
}