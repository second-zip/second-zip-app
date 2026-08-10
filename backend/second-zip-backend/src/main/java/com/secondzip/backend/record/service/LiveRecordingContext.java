package com.secondzip.backend.record.service;

import com.secondzip.backend.record.dto.response.ChecklistAnalysisResult;
import com.secondzip.backend.record.enums.ChecklistAnalysisStatus;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

//실시간 녹음 한 건의 작업 메모리 지금까지의 transcript, 마지막 분석 위치, GPT 분석 중 여부, PROVISIONAL 결과를 보관
@Getter
public class LiveRecordingContext {

    private final String category;

    private final StringBuilder transcript = new StringBuilder();

    // 마지막으로 GPT 분석했던 transcript 길이
    private int lastAnalyzedLength = 0;

    private final AtomicBoolean analyzing = new AtomicBoolean(false);

    // 중간 분석 결과
    private final Map<Long, ChecklistAnalysisResult.ResultItem> provisionalResults = new ConcurrentHashMap<>();

    public LiveRecordingContext(String category) {
        this.category = category;
    }

    public synchronized void appendTranscript(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        if (transcript.length() > 0) {
            transcript.append(" ");
        }

        transcript.append(text.trim());
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