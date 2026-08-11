package com.secondzip.backend.record.service;

import com.secondzip.backend.record.mapper.RecordingSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordingFinalAnalysisAsyncService {

    private final ChecklistAnalysisService checklistAnalysisService;

    private final RecordingSessionMapper recordingSessionMapper;


    @Async
    public void analyze(
            Long recordingSessionId
    ) {
        try {
            checklistAnalysisService.analyze(recordingSessionId);
        } catch (Exception e) {
            recordingSessionMapper.markFailed(recordingSessionId, safeFailureReason(e));
        }
    }


    private String safeFailureReason(
            Exception e
    ) {
        String message = e.getMessage();


        if (message == null || message.isBlank()) {
            return "최종 체크리스트 분석 중 오류가 발생했습니다.";
        }


        return message.length() > 1000
                ? message.substring(
                        0,
                        1000
                )
                : message;
    }
}