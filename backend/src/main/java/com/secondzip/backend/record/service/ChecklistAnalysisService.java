package com.secondzip.backend.record.service;

import com.secondzip.backend.checklist.mapper.ReportChecklistMapper;
import com.secondzip.backend.record.mapper.ChecklistItemMapper;
import com.secondzip.backend.record.client.ChecklistAnalysisClient;
import com.secondzip.backend.record.domain.RecordingSessionVO;
import com.secondzip.backend.record.dto.request.ChecklistItemInput;
import com.secondzip.backend.record.dto.response.ChecklistAnalysisResult;
import com.secondzip.backend.record.enums.ChecklistAnalysisStatus;
import com.secondzip.backend.record.enums.RecordingStatus;
import com.secondzip.backend.record.mapper.ChecklistAnalysisResultMapper;
import com.secondzip.backend.record.mapper.RecordingSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

//녹음이 끝난 후 전체 transcript를 GPT로 최종 분석하고 DB 및 회원 체크리스트까지 최종 반영
@Service
@RequiredArgsConstructor
public class ChecklistAnalysisService {

    private final ReportChecklistMapper reportChecklistMapper;

    private final RecordingSessionMapper recordingSessionMapper;

    private final ChecklistItemMapper checklistItemMapper;

    private final ChecklistAnalysisClient checklistAnalysisClient;

    private final ChecklistAnalysisResultMapper resultMapper;


    @Transactional
    public void analyze(
            Long recordingSessionId) {

        RecordingSessionVO session =
                recordingSessionMapper.findById(
                        recordingSessionId
                );


        validateSession(session);


        if (session.getReportChecklistId() == null) {

            throw new IllegalStateException(
                    "녹음과 연결된 체크리스트가 없습니다."
            );
        }


        List<ChecklistItemInput> checklistItems =
                checklistItemMapper
                        .findByReportChecklistId(
                                session.getReportChecklistId()
                        );


        if (checklistItems == null
                || checklistItems.isEmpty()) {

            recordingSessionMapper.updateSummary(
                    recordingSessionId,
                    "분석할 체크리스트 항목이 없습니다."
            );

            recordingSessionMapper.updateStatus(
                    recordingSessionId,
                    RecordingStatus.COMPLETED
            );

            return;
        }


        ChecklistAnalysisResult result =
                checklistAnalysisClient.analyze(
                        session.getFullTranscript(),
                        checklistItems
                );


        validateAnalysisResult(
                checklistItems,
                result
        );


        recordingSessionMapper.updateSummary(
                recordingSessionId,
                result.getSummary()
        );


        for (ChecklistAnalysisResult.ResultItem item
                : result.getResults()) {

            // 녹음 분석 결과 자체는 기록
            resultMapper.upsertResult(
                    recordingSessionId,
                    item
            );


            // 확실하게 확인된 항목만 기존 체크리스트에 추가 체크
            if (item.getStatus()
                    == ChecklistAnalysisStatus.CHECKED) {

                reportChecklistMapper.markChecked(
                        session.getAccountId(),
                        session.getReportChecklistId(),
                        item.getChecklistItemId()
                );
            }
        }


        recordingSessionMapper.updateStatus(
                recordingSessionId,
                RecordingStatus.COMPLETED
        );
    }


    private void validateSession(
            RecordingSessionVO session
    ) {

        if (session == null) {

            throw new IllegalStateException(
                    "녹음 세션을 찾을 수 없습니다."
            );
        }


        if (session.getFullTranscript() == null
                || session.getFullTranscript().isBlank()) {

            throw new IllegalStateException(
                    "분석할 녹취문이 없습니다."
            );
        }


        if (session.getStatus()
                != RecordingStatus.ANALYZING) {

            throw new IllegalStateException(
                    "AI 분석이 가능한 상태가 아닙니다."
            );
        }
    }


    private void validateAnalysisResult(
            List<ChecklistItemInput> requestedItems,
            ChecklistAnalysisResult result
    ) {

        if (result == null) {

            throw new IllegalStateException(
                    "AI 분석 응답이 없습니다."
            );
        }


        if (result.getSummary() == null
                || result.getSummary().isBlank()) {

            throw new IllegalStateException(
                    "AI 분석 요약이 없습니다."
            );
        }


        if (result.getResults() == null) {

            throw new IllegalStateException(
                    "AI 체크리스트 분석 결과가 없습니다."
            );
        }


        Set<Long> requestedIds =
                requestedItems.stream()
                        .map(
                                ChecklistItemInput
                                        ::getChecklistItemId
                        )
                        .collect(
                                Collectors.toSet()
                        );


        Set<Long> returnedIds =
                new HashSet<>();


        for (
                ChecklistAnalysisResult.ResultItem item
                : result.getResults()
        ) {

            validateResultItem(
                    requestedIds,
                    returnedIds,
                    item
            );
        }


        if (!returnedIds.equals(requestedIds)) {

            throw new IllegalStateException(
                    "AI 응답에 일부 체크리스트 항목이 누락되었습니다."
            );
        }
    }


    private void validateResultItem(
            Set<Long> requestedIds,
            Set<Long> returnedIds,
            ChecklistAnalysisResult.ResultItem item
    ) {

        if (item == null) {

            throw new IllegalStateException(
                    "AI 체크리스트 결과가 비어 있습니다."
            );
        }


        Long checklistItemId =
                item.getChecklistItemId();


        if (checklistItemId == null) {

            throw new IllegalStateException(
                    "AI 응답에 체크리스트 ID가 없습니다."
            );
        }


        if (!requestedIds.contains(
                checklistItemId
        )) {

            throw new IllegalStateException(
                    "AI가 요청하지 않은 체크리스트 ID를 반환했습니다. "
                            + "checklistItemId="
                            + checklistItemId
            );
        }


        if (!returnedIds.add(
                checklistItemId
        )) {

            throw new IllegalStateException(
                    "AI가 동일한 체크리스트 ID를 중복 반환했습니다. "
                            + "checklistItemId="
                            + checklistItemId
            );
        }


        if (item.getStatus() == null) {

            throw new IllegalStateException(
                    "AI 체크리스트 상태가 없습니다."
            );
        }


        BigDecimal confidenceScore =
                item.getConfidenceScore();


        if (confidenceScore == null
                || confidenceScore.compareTo(
                BigDecimal.ZERO
        ) < 0
                || confidenceScore.compareTo(
                BigDecimal.ONE
        ) > 0) {

            throw new IllegalStateException(
                    "AI confidenceScore는 0 이상 1 이하여야 합니다."
            );
        }


        if (item.getReason() == null
                || item.getReason().isBlank()) {

            throw new IllegalStateException(
                    "AI 판단 사유가 없습니다."
            );
        }
    }
}