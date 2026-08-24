package com.secondzip.backend.report.record.service;

import com.secondzip.backend.record.service.LiveChecklistAnalysisAsyncService;
import com.secondzip.backend.record.service.LiveChecklistAnalysisService;
import com.secondzip.backend.record.service.LiveRecordingContext;
import com.secondzip.backend.record.service.LiveRecordingContextManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveChecklistAnalysisAsyncServiceTest {

    @Mock
    private LiveChecklistAnalysisService liveChecklistAnalysisService;

    @Mock
    private LiveRecordingContextManager contextManager;

    @Mock
    private LiveRecordingContext context;

    @InjectMocks
    private LiveChecklistAnalysisAsyncService asyncService;

    @Test
    void analyze_success_finishesAnalysis() {
        // given
        when(contextManager.find(10L))
                .thenReturn(context);

        // when
        asyncService.analyze(
                10L,
                "등기부등본을 확인했습니다."
        );

        // then
        verify(liveChecklistAnalysisService)
                .analyzeProvisional(
                        10L,
                        "등기부등본을 확인했습니다."
                );

        verify(context)
                .finishAnalysis();
    }

    @Test
    void analyze_failure_stillFinishesAnalysis() {
        // given
        doThrow(
                new RuntimeException("GPT 오류")
        )
                .when(liveChecklistAnalysisService)
                .analyzeProvisional(
                        10L,
                        "transcript"
                );

        when(contextManager.find(10L))
                .thenReturn(context);

        // when & then
        assertDoesNotThrow(
                () -> asyncService.analyze(
                        10L,
                        "transcript"
                )
        );

        verify(context)
                .finishAnalysis();
    }

    @Test
    void analyze_contextAlreadyRemoved_doesNotFail() {
        // given
        when(contextManager.find(10L))
                .thenReturn(null);

        // when & then
        assertDoesNotThrow(
                () -> asyncService.analyze(
                        10L,
                        "transcript"
                )
        );
    }
}