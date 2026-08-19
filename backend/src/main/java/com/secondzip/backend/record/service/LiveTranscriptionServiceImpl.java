package com.secondzip.backend.record.service;

import com.secondzip.backend.record.client.ClovaRealtimeSpeechClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class LiveTranscriptionServiceImpl implements LiveTranscriptionService {

    private final LiveRecordingContextManager contextManager;
    private final LiveChecklistAnalysisAsyncService liveChecklistAnalysisAsyncService;
    private static final int ANALYSIS_THRESHOLD = 300;
    private final ClovaRealtimeSpeechClient clovaRealtimeSpeechClient;
    @Override
    public void start(Long recordingSessionId) {

        contextManager.get(recordingSessionId);

        clovaRealtimeSpeechClient.start(
                recordingSessionId,
                (position, text) -> onTranscript(
                        recordingSessionId,
                        position,
                        text
                )
        );
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

        clovaRealtimeSpeechClient.sendAudio(recordingSessionId, audioChunk);
    }

    //CLOVA 응답 callback
    public void onTranscript(
            Long recordingSessionId,
            Integer position, String text
    ) {

        LiveRecordingContext context =
                contextManager.get(recordingSessionId);

        if (context == null) {
            return;
        }

        context.appendTranscript(position,text);

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

        clovaRealtimeSpeechClient.finish(recordingSessionId);


        LiveRecordingContext context =
                contextManager.remove(recordingSessionId);

        if (context == null) {
            return "";
        }

        return context.getTranscript();
    }
}