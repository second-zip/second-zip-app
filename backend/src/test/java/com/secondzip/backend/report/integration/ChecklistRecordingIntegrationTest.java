package com.secondzip.backend.report.integration;

import com.secondzip.backend.checklist.dto.response.ChecklistAddressDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistDetailResponseDTO;
import com.secondzip.backend.checklist.dto.response.ReportChecklistConditionDTO;
import com.secondzip.backend.checklist.enums.Category;
import com.secondzip.backend.checklist.mapper.ReportChecklistMapper;
import com.secondzip.backend.checklist.service.ChecklistService;
import com.secondzip.backend.checklist.service.ChecklistServiceImpl;
import com.secondzip.backend.record.domain.RecordingSession;
import com.secondzip.backend.record.dto.response.RecordingLiveStartResponseDTO;
import com.secondzip.backend.record.enums.RecordingStatus;
import com.secondzip.backend.record.mapper.RecordingSessionMapper;
import com.secondzip.backend.record.service.*;
import com.secondzip.backend.record.storage.RecordingStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistRecordingIntegrationTest {

    @Mock
    private ReportChecklistMapper reportChecklistMapper;

    @Mock
    private RecordingSessionMapper recordingSessionMapper;

    @Mock
    private RecordingStorage recordingStorage;

    @Mock
    private RecordingAsyncService recordingAsyncService;

    @Mock
    private LiveTranscriptionService liveTranscriptionService;

    @Mock
    private ChecklistAnalysisService checklistAnalysisService;

    private LiveRecordingContextManager contextManager;

    private ChecklistService checklistService;
    private RecordingService recordingService;

    @BeforeEach
    void setUp() {
        contextManager =
                new LiveRecordingContextManager();

        checklistService =
                new ChecklistServiceImpl(
                        reportChecklistMapper,
                        recordingSessionMapper
                );

        recordingService =
                new RecordingServiceImpl(
                        recordingStorage,
                        recordingSessionMapper,
                        recordingAsyncService,
                        contextManager,
                        reportChecklistMapper,
                        liveTranscriptionService,
                        checklistAnalysisService
                );
    }

    @Test
    @DisplayName("생성된 체크리스트에서 실시간 녹음을 시작하면 상세조회에 recordingSessionId가 연결된다")
    void createChecklist_thenStartRecording_linksRecordingSession() {
        // given
        Long accountId = 1L;
        Long analysisReportId = 10L;

        ReportChecklistConditionDTO condition =
                new ReportChecklistConditionDTO();

        ReflectionTestUtils.setField(
                condition,
                "housingCategory",
                Category.APARTMENT
        );

        ReflectionTestUtils.setField(
                condition,
                "trustProperty",
                false
        );

        when(reportChecklistMapper.findReportCondition(
                analysisReportId,
                accountId
        )).thenReturn(condition);

        when(reportChecklistMapper
                .findChecklistIdByReportId(
                        analysisReportId
                ))
                .thenReturn(null);

        when(reportChecklistMapper.insertChecklist(any()))
                .thenAnswer(invocation -> {

                    Object checklist =
                            invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            checklist,
                            "reportChecklistId",
                            100L
                    );

                    return 1;
                });

        AtomicReference<RecordingSession>
                savedRecording =
                new AtomicReference<>();

        when(recordingSessionMapper
                .insertLiveSession(any()))
                .thenAnswer(invocation -> {

                    RecordingSession session =
                            invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            session,
                            "recordingSessionId",
                            200L
                    );

                    savedRecording.set(session);

                    return 1;
                });

        when(reportChecklistMapper
                .existsOwnedChecklist(
                        accountId,
                        100L
                ))
                .thenReturn(1);

        ChecklistAddressDTO address =
                mock(ChecklistAddressDTO.class);

        when(address.getRoadAddress())
                .thenReturn(
                        "서울특별시 강남구 테헤란로"
                );

        when(address.getDetailAddress())
                .thenReturn("101동 101호");

        when(reportChecklistMapper
                .findChecklistAddress(
                        accountId,
                        100L
                ))
                .thenReturn(address);

        when(reportChecklistMapper
                .findChecklistItems(
                        accountId,
                        100L
                ))
                .thenReturn(List.of());

        when(recordingSessionMapper
                .findByReportChecklistId(100L))
                .thenAnswer(
                        invocation ->
                                savedRecording.get()
                );

        // when 1 - 체크리스트 생성
        Long reportChecklistId =
                checklistService.createChecklist(
                        accountId,
                        analysisReportId
                );

        // when 2 - 해당 체크리스트로 실시간 녹음 시작
        RecordingLiveStartResponseDTO recording =
                recordingService.startLiveRecording(
                        accountId,
                        reportChecklistId
                );

        // when 3 - 체크리스트 상세조회
        ChecklistDetailResponseDTO detail =
                checklistService.getChecklist(
                        accountId,
                        reportChecklistId
                );

        // then
        assertEquals(
                100L,
                reportChecklistId
        );

        assertEquals(
                200L,
                recording.getRecordingSessionId()
        );

        assertEquals(
                RecordingStatus.RECORDING,
                recording.getStatus()
        );

        assertEquals(
                200L,
                detail.getRecordingSessionId()
        );

        // 실시간 Context까지 생성됐는지
        assertNotNull(
                contextManager.get(200L)
        );
    }
}