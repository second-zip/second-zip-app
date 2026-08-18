package com.secondzip.backend.record.service;

import com.secondzip.backend.record.dto.response.ChecklistAnalysisResult;
import com.secondzip.backend.record.enums.ChecklistAnalysisStatus;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

//실시간 녹음 한 건의 작업 메모리 지금까지의 transcript, 마지막 분석 위치, GPT 분석 중 여부, PROVISIONAL 결과를 보관
@Getter
@Slf4j
public class LiveRecordingContext {

    private final StringBuilder transcript = new StringBuilder();

    // 마지막으로 GPT 분석했던 transcript 길이
    private int lastAnalyzedLength = 0;

    private final AtomicBoolean analyzing = new AtomicBoolean(false);

    // 중간 분석 결과
    private final Map<Long, ChecklistAnalysisResult.ResultItem> provisionalResults = new ConcurrentHashMap<>();


    public synchronized void appendTranscript(int position, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        if (position < 0) {
            return;
        }

        // 정상적으로 뒤에 이어지는 경우
        if (position == transcript.length()) {
            transcript.append(text);
            return;
        }

        // CLOVA가 이전 인식 결과를 수정해서 보내는 경우
        if (position < transcript.length()) {

            int endPosition = Math.min(
                    position + text.length(),
                    transcript.length()
            );

            transcript.replace(
                    position,
                    endPosition,
                    text
            );

            return;
        }

        // 정상적이라면 거의 발생하지 않아야 함
        log.warn(
                "Transcript position gap. position={}, currentLength={}, text=[{}]",
                position,
                transcript.length(),
                text
        );

        transcript.append(text);
    }

    public synchronized String getTranscript() {
        return transcript.toString();
    }

    public synchronized int getUnanalyzedLength() {
        return transcript.length() - lastAnalyzedLength;
    }

    public synchronized void markAnalyzed() {
        this.lastAnalyzedLength = transcript.length();
    }

    public boolean startAnalysis() {
        return analyzing.compareAndSet(
                false,
                true
        );
    }


    public void finishAnalysis() {
        analyzing.set(false);
    }


    public void updateProvisional(
            ChecklistAnalysisResult.ResultItem item
    ) {

        ChecklistAnalysisResult.ResultItem provisional =
                ChecklistAnalysisResult.ResultItem.builder()
                        .checklistItemId(
                                item.getChecklistItemId()
                        )
                        .status(
                                ChecklistAnalysisStatus.PROVISIONAL
                        )
                        .confidenceScore(
                                item.getConfidenceScore()
                        )
                        .evidenceText(
                                item.getEvidenceText()
                        )
                        .reason(
                                item.getReason()
                        )
                        .build();

        provisionalResults.put(
                item.getChecklistItemId(),
                provisional
        );
    }


    public Map<Long, ChecklistAnalysisResult.ResultItem>
    getProvisionalResults() {
        return provisionalResults;
    }
}