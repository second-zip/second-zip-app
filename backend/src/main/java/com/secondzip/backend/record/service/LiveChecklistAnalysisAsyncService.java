package com.secondzip.backend.record.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

//STOP 후 Context가 없어져도 async thread가 예외 생성X
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveChecklistAnalysisAsyncService {

    private final LiveChecklistAnalysisService liveChecklistAnalysisService;
    private final LiveRecordingContextManager contextManager;

    @Async
    public void analyze(
            Long recordingSessionId,
            String transcript
    ) {
        try {
            liveChecklistAnalysisService.analyzeProvisional(
                    recordingSessionId,
                    transcript
            );
        } catch (Exception e) {

            log.warn(
                    "실시간 중간 GPT 분석 실패. recordingSessionId={}",
                    recordingSessionId,
                    e
            );

        } finally {

            LiveRecordingContext context =
                    contextManager.find(
                            recordingSessionId
                    );


            if (context != null) {

                context.finishAnalysis();
            }
        }
    }
}