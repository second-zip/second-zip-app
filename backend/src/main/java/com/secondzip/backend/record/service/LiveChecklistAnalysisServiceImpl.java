package com.secondzip.backend.record.service;

import com.secondzip.backend.record.client.ChecklistAnalysisClient;
import com.secondzip.backend.record.domain.RecordingSession;
import com.secondzip.backend.record.dto.request.ChecklistItemInput;
import com.secondzip.backend.record.dto.response.ChecklistAnalysisResult;
import com.secondzip.backend.record.enums.ChecklistAnalysisStatus;
import com.secondzip.backend.record.mapper.ChecklistItemMapper;
import com.secondzip.backend.record.mapper.RecordingSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveChecklistAnalysisServiceImpl
        implements LiveChecklistAnalysisService {

    private final ChecklistAnalysisClient checklistAnalysisClient;
    private final ChecklistItemMapper checklistItemMapper;
    private final RecordingSessionMapper recordingSessionMapper;
    private final LiveRecordingContextManager contextManager;

    @Override
    public void analyzeProvisional(
            Long recordingSessionId,
            String transcript
    ) {
        RecordingSession recordingSession =
                recordingSessionMapper.findById(recordingSessionId);

        if (recordingSession == null || recordingSession.getReportChecklistId() == null) {
            throw new IllegalStateException(
                    "녹음과 연결된 체크리스트가 없습니다."
            );
        }

        List<ChecklistItemInput> checklistItems =
                checklistItemMapper
                        .findByReportChecklistId(
                                recordingSession.getReportChecklistId()
                        );

        if (checklistItems == null
                || checklistItems.isEmpty()) {

            throw new IllegalStateException(
                    "실시간 분석할 체크리스트 항목이 없습니다."
            );
        }

        ChecklistAnalysisResult result =
                checklistAnalysisClient.analyze(
                        transcript,
                        checklistItems
                );

        if (result == null
                || result.getResults() == null) {

            throw new IllegalStateException(
                    "실시간 AI 분석 결과가 없습니다."
            );
        }

        LiveRecordingContext context = contextManager.find(recordingSessionId);

        if (context == null) {
            return;
        }

        for (ChecklistAnalysisResult.ResultItem item : result.getResults()) {
            /*
             * 실시간 분석에서는 GPT가 CHECKED라고 판단해도
             * 실제 회원 체크리스트에는 반영하지 않는다.
             *
             * 최종 검증 전 후보 상태인 PROVISIONAL로만 보관한다.
             */
            if (item.getStatus() == ChecklistAnalysisStatus.CHECKED) {

                context.updateProvisional(item);

                log.info(
                        "실시간 체크리스트 후보 감지. recordingSessionId={}, checklistItemId={}, confidence={}",
                        recordingSessionId,
                        item.getChecklistItemId(),
                        item.getConfidenceScore()
                );
            }
        }
    }
}