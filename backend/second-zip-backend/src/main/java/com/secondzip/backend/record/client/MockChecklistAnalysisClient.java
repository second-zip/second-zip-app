package com.secondzip.backend.record.client;

import com.secondzip.backend.record.dto.request.ChecklistItemInput;
import com.secondzip.backend.record.dto.response.ChecklistAnalysisResult;
import com.secondzip.backend.record.enums.ChecklistAnalysisStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MockChecklistAnalysisClient
        implements ChecklistAnalysisClient {

    @Override
    public ChecklistAnalysisResult analyze(
            String transcript,
            List<ChecklistItemInput> checklistItems
    ) {

        List<ChecklistAnalysisResult.ResultItem> results =
                checklistItems.stream()
                        .map(item ->
                                ChecklistAnalysisResult
                                        .ResultItem
                                        .builder()
                                        .checklistItemId(
                                                item.getChecklistItemId()
                                        )
                                        .status(
                                                ChecklistAnalysisStatus
                                                        .CHECKED
                                        )
                                        .confidenceScore(
                                                new BigDecimal(
                                                        "0.9000"
                                                )
                                        )
                                        .evidenceText(
                                                transcript
                                        )
                                        .reason(
                                                "Mock AI 분석 결과입니다."
                                        )
                                        .build()
                        )
                        .collect(
                                Collectors.toList()
                        );


        return ChecklistAnalysisResult
                .builder()
                .summary(
                        "Mock 체크리스트 분석이 완료되었습니다."
                )
                .results(results)
                .build();
    }
}