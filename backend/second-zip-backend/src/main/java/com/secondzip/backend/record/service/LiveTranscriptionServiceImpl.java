package com.secondzip.backend.record.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class LiveTranscriptionServiceImpl
        implements LiveTranscriptionService {

    private final LiveRecordingContextManager contextManager;
    private final LiveChecklistAnalysisAsyncService liveChecklistAnalysisAsyncService;
    private static final int ANALYSIS_THRESHOLD = 300;

    @Override
    public void start(Long recordingSessionId) {

        contextManager.create(recordingSessionId);

        // 여기서 CLOVA 실시간 STT 연결 시작
    }

    //CLOVA에 byte[] 전송
    @Override
    public void acceptAudio(
            Long recordingSessionId,
            byte[] audioChunk
    ) {

        LiveRecordingContext context =
                contextManager.get(recordingSessionId);

        if (context == null) {
            throw new IllegalStateException(
                    "실시간 녹음 세션이 존재하지 않습니다."
            );
        }

        // 다음:
        // CLOVA 실시간 STT에 audioChunk 전달
    }

    //CLOVA 응답 callback
    public void onTranscript(
            Long recordingSessionId,
            String text
    ) {

        LiveRecordingContext context =
                contextManager.get(recordingSessionId);

        if (context == null) {
            return;
        }

        context.appendTranscript(text);

        log.info(
                "실시간 STT 결과. recordingSessionId={}, text={}",
                recordingSessionId,
                text
        );

        if (context.getUnanalyzedLength() < ANALYSIS_THRESHOLD) {
            return;
        }

        if (!context.startAnalysis()) {
            return;
        }

        String transcript =
                context.getTranscript();

        context.markAnalyzed();

        liveChecklistAnalysisAsyncService.analyze(
                recordingSessionId,
                transcript
        );
    }

    @Override
    public String finish(Long recordingSessionId) {

        LiveRecordingContext context =
                contextManager.remove(recordingSessionId);

        if (context == null) {
            return "";
        }

        return context.getTranscript();
    }
}