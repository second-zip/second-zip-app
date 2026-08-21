package com.secondzip.backend.report.record.service;

import com.secondzip.backend.checklist.mapper.ReportChecklistMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.record.domain.RecordingSession;
import com.secondzip.backend.record.dto.response.RecordingLiveStartResponseDTO;
import com.secondzip.backend.record.dto.response.RecordingSessionResponseDTO;
import com.secondzip.backend.record.dto.response.RecordingStatusResponseDTO;
import com.secondzip.backend.record.dto.response.RecordingTranscriptResponseDTO;
import com.secondzip.backend.record.enums.RecordingStatus;
import com.secondzip.backend.record.mapper.RecordingSessionMapper;
import com.secondzip.backend.record.service.*;
import com.secondzip.backend.record.storage.RecordingStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordingServiceImplTest {

    @Mock
    private RecordingStorage recordingStorage;

    @Mock
    private RecordingSessionMapper recordingSessionMapper;

    @Mock
    private RecordingAsyncService recordingAsyncService;

    @Mock
    private LiveRecordingContextManager contextManager;

    @Mock
    private ReportChecklistMapper reportChecklistMapper;

    @Mock
    private LiveTranscriptionService liveTranscriptionService;

    @Mock
    private ChecklistAnalysisService checklistAnalysisService;

    @InjectMocks
    private RecordingServiceImpl recordingService;


    /*
     * =========================================================
     * 파일 업로드 + 세션 생성
     * =========================================================
     */
    @Nested
    @DisplayName("녹음 세션 생성")
    class CreateSession {

        @Test
        @DisplayName("정상 녹음 파일을 업로드하면 UPLOADED 상태의 세션을 생성한다")
        void createSession_success() {
            // given
            Long accountId = 1L;
            Long reportChecklistId = 100L;

            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "recording.mp3",
                            "audio/mpeg",
                            "audio-data".getBytes()
                    );

            when(reportChecklistMapper.existsOwnedChecklist(
                    accountId,
                    reportChecklistId
            )).thenReturn(1);

            when(recordingStorage.upload(
                    accountId,
                    file
            )).thenReturn(
                    "recordings/1/recording.mp3"
            );

            when(recordingSessionMapper.insert(
                    any(RecordingSession.class)
            )).thenAnswer(invocation -> {

                RecordingSession session =
                        invocation.getArgument(0);

                // 실제 MyBatis generated key 동작 흉내
                ReflectionTestUtils.setField(
                        session,
                        "recordingSessionId",
                        10L
                );

                return 1;
            });

            // when
            RecordingSessionResponseDTO result =
                    recordingService.createSession(
                            accountId,
                            reportChecklistId,
                            file
                    );

            // then
            assertEquals(
                    10L,
                    result.getRecordingSessionId()
            );

            assertEquals(
                    RecordingStatus.UPLOADED,
                    result.getStatus()
            );

            ArgumentCaptor<RecordingSession> captor =
                    ArgumentCaptor.forClass(
                            RecordingSession.class
                    );

            verify(recordingSessionMapper)
                    .insert(captor.capture());

            RecordingSession saved =
                    captor.getValue();

            assertEquals(
                    accountId,
                    saved.getAccountId()
            );

            assertEquals(
                    reportChecklistId,
                    saved.getReportChecklistId()
            );

            assertEquals(
                    "recording.mp3",
                    saved.getOriginalFileName()
            );

            assertEquals(
                    "recordings/1/recording.mp3",
                    saved.getStorageObjectKey()
            );

            assertEquals(
                    RecordingStatus.UPLOADED,
                    saved.getStatus()
            );
        }


        @Test
        @DisplayName("로그인하지 않은 사용자는 녹음 세션을 생성할 수 없다")
        void createSession_noAccount_throwsException() {
            // given
            MockMultipartFile file =
                    validFile();

            // when
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> recordingService.createSession(
                                    null,
                                    100L,
                                    file
                            )
                    );

            // then
            assertEquals(
                    ErrorCode.UNAUTHORIZED,
                    exception.getErrorCode()
            );

            verifyNoInteractions(recordingStorage);
            verifyNoInteractions(recordingSessionMapper);
        }


        @Test
        @DisplayName("본인의 체크리스트가 아니면 녹음 세션 생성에 실패한다")
        void createSession_checklistNotFound_throwsException() {
            // given
            when(reportChecklistMapper.existsOwnedChecklist(
                    1L,
                    100L
            )).thenReturn(0);

            // when
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> recordingService.createSession(
                                    1L,
                                    100L,
                                    validFile()
                            )
                    );

            // then
            assertEquals(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    exception.getErrorCode()
            );

            verifyNoInteractions(recordingStorage);
        }


        @Test
        @DisplayName("녹음 파일이 비어 있으면 업로드하지 않는다")
        void createSession_emptyFile_throwsException() {
            // given
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "recording.mp3",
                            "audio/mpeg",
                            new byte[0]
                    );

            when(reportChecklistMapper.existsOwnedChecklist(
                    1L,
                    100L
            )).thenReturn(1);

            // when
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> recordingService.createSession(
                                    1L,
                                    100L,
                                    file
                            )
                    );

            // then
            assertEquals(
                    ErrorCode.INVALID_REQUEST,
                    exception.getErrorCode()
            );

            verifyNoInteractions(recordingStorage);
        }


        @Test
        @DisplayName("지원하지 않는 파일 확장자는 업로드하지 않는다")
        void createSession_invalidExtension_throwsException() {
            // given
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "recording.txt",
                            "text/plain",
                            "test".getBytes()
                    );

            when(reportChecklistMapper.existsOwnedChecklist(
                    1L,
                    100L
            )).thenReturn(1);

            // when
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> recordingService.createSession(
                                    1L,
                                    100L,
                                    file
                            )
                    );

            // then
            assertEquals(
                    ErrorCode.INVALID_REQUEST,
                    exception.getErrorCode()
            );

            verifyNoInteractions(recordingStorage);
        }


        @Test
        @DisplayName("DB 세션 저장에 실패하면 예외가 발생한다")
        void createSession_insertFailed_throwsException() {
            // given
            MockMultipartFile file =
                    validFile();

            when(reportChecklistMapper.existsOwnedChecklist(
                    1L,
                    100L
            )).thenReturn(1);

            when(recordingStorage.upload(
                    1L,
                    file
            )).thenReturn("object-key");

            when(recordingSessionMapper.insert(any()))
                    .thenReturn(0);

            // when
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> recordingService.createSession(
                                    1L,
                                    100L,
                                    file
                            )
                    );

            // then
            assertEquals(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    exception.getErrorCode()
            );
        }
    }


    /*
     * =========================================================
     * 파일 방식 STT 시작
     * =========================================================
     */
    @Nested
    @DisplayName("Transcription 시작")
    class StartTranscription {

        @Test
        @DisplayName("UPLOADED 상태의 녹음은 비동기 STT를 시작한다")
        void startTranscription_success() {
            // given
            RecordingSession session =
                    RecordingSession.builder()
                            .recordingSessionId(10L)
                            .accountId(1L)
                            .storageObjectKey(
                                    "recordings/test.mp3"
                            )
                            .status(
                                    RecordingStatus.UPLOADED
                            )
                            .build();

            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(session);

            // when
            recordingService.startTranscription(
                    1L,
                    10L
            );

            // then
            verify(recordingAsyncService)
                    .transcribe(
                            10L,
                            "recordings/test.mp3"
                    );
        }


        @Test
        @DisplayName("녹음 세션이 없으면 STT를 시작할 수 없다")
        void startTranscription_notFound_throwsException() {
            // given
            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(null);

            // when
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () ->
                                    recordingService
                                            .startTranscription(
                                                    1L,
                                                    10L
                                            )
                    );

            // then
            assertEquals(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    exception.getErrorCode()
            );

            verifyNoInteractions(recordingAsyncService);
        }


        @Test
        @DisplayName("UPLOADED 상태가 아니면 STT를 시작할 수 없다")
        void startTranscription_invalidStatus_throwsException() {
            // given
            RecordingSession session =
                    RecordingSession.builder()
                            .recordingSessionId(10L)
                            .accountId(1L)
                            .status(
                                    RecordingStatus.COMPLETED
                            )
                            .build();

            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(session);

            // when
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () ->
                                    recordingService
                                            .startTranscription(
                                                    1L,
                                                    10L
                                            )
                    );

            // then
            assertEquals(
                    ErrorCode.INVALID_REQUEST,
                    exception.getErrorCode()
            );

            verifyNoInteractions(recordingAsyncService);
        }
    }


    /*
     * =========================================================
     * 실시간 녹음 시작
     * =========================================================
     */
    @Nested
    @DisplayName("실시간 녹음 시작")
    class StartLiveRecording {

        @Test
        @DisplayName("실시간 녹음을 시작하면 RECORDING 세션과 Context를 생성한다")
        void startLiveRecording_success() {
            // given
            when(reportChecklistMapper.existsOwnedChecklist(
                    1L,
                    100L
            )).thenReturn(1);

            when(recordingSessionMapper.insertLiveSession(
                    any(RecordingSession.class)
            )).thenAnswer(invocation -> {

                RecordingSession session =
                        invocation.getArgument(0);

                ReflectionTestUtils.setField(
                        session,
                        "recordingSessionId",
                        10L
                );

                return 1;
            });

            // when
            RecordingLiveStartResponseDTO result =
                    recordingService.startLiveRecording(
                            1L,
                            100L
                    );

            // then
            assertEquals(
                    10L,
                    result.getRecordingSessionId()
            );

            assertEquals(
                    RecordingStatus.RECORDING,
                    result.getStatus()
            );

            verify(contextManager)
                    .create(10L);

            ArgumentCaptor<RecordingSession> captor =
                    ArgumentCaptor.forClass(
                            RecordingSession.class
                    );

            verify(recordingSessionMapper)
                    .insertLiveSession(
                            captor.capture()
                    );

            assertEquals(
                    RecordingStatus.RECORDING,
                    captor.getValue().getStatus()
            );
        }


        @Test
        @DisplayName("본인의 체크리스트가 아니면 실시간 녹음을 시작할 수 없다")
        void startLiveRecording_checklistNotFound_throwsException() {
            // given
            when(reportChecklistMapper.existsOwnedChecklist(
                    1L,
                    100L
            )).thenReturn(0);

            // when
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () ->
                                    recordingService
                                            .startLiveRecording(
                                                    1L,
                                                    100L
                                            )
                    );

            // then
            assertEquals(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    exception.getErrorCode()
            );

            verifyNoInteractions(contextManager);
        }
    }


    /*
     * =========================================================
     * 실시간 녹음 종료
     * =========================================================
     */
    @Nested
    @DisplayName("실시간 녹음 종료")
    class StopLiveRecording {

        @Test
        @DisplayName("파일과 녹취가 있으면 파일 저장 후 최종 STT를 시작한다")
        void stopLiveRecording_success() {
            // given
            RecordingSession session =
                    recordingSession(
                            RecordingStatus.RECORDING
                    );

            MockMultipartFile file =
                    validFile();

            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(session);

            when(liveTranscriptionService.finish(10L))
                    .thenReturn(
                            "계약 내용을 확인했습니다."
                    );

            when(recordingStorage.upload(
                    1L,
                    file
            )).thenReturn(
                    "recordings/final.mp3"
            );

            // when
            recordingService.stopLiveRecording(
                    1L,
                    10L,
                    file
            );

            // then
            verify(liveTranscriptionService)
                    .finish(10L);

            verify(recordingStorage)
                    .upload(
                            1L,
                            file
                    );

            verify(recordingSessionMapper)
                    .updateFileInfo(
                            10L,
                            "recording.mp3",
                            "recordings/final.mp3",
                            "audio/mpeg",
                            file.getSize()
                    );

            verify(recordingAsyncService)
                    .transcribe(
                            10L,
                            "recordings/final.mp3"
                    );
        }


        @Test
        @DisplayName("파일 없이 종료해도 녹취가 있으면 ANALYZING 상태로 전환하고 최종 분석한다")
        void stopLiveRecording_noFileWithTranscript_analyzes() {
            // given
            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(
                            recordingSession(
                                    RecordingStatus.RECORDING
                            )
                    );

            when(liveTranscriptionService.finish(10L))
                    .thenReturn(
                            "등기부등본을 확인했습니다."
                    );

            // when
            recordingService.stopLiveRecording(
                    1L,
                    10L,
                    null
            );

            // then
            verify(recordingSessionMapper)
                    .updateTranscript(
                            10L,
                            "등기부등본을 확인했습니다.",
                            RecordingStatus.ANALYZING
                    );

            verify(checklistAnalysisService)
                    .analyze(10L);

            verifyNoInteractions(recordingStorage);
            verifyNoInteractions(recordingAsyncService);
        }


        @Test
        @DisplayName("파일도 녹취도 없으면 COMPLETED 상태로 종료한다")
        void stopLiveRecording_noFileNoTranscript_completes() {
            // given
            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(
                            recordingSession(
                                    RecordingStatus.RECORDING
                            )
                    );

            when(liveTranscriptionService.finish(10L))
                    .thenReturn("");

            // when
            recordingService.stopLiveRecording(
                    1L,
                    10L,
                    null
            );

            // then
            verify(recordingSessionMapper)
                    .updateStatus(
                            10L,
                            RecordingStatus.COMPLETED
                    );

            verifyNoInteractions(checklistAnalysisService);
            verifyNoInteractions(recordingStorage);
        }


        @Test
        @DisplayName("파일은 있지만 녹취 내용이 없으면 종료에 실패한다")
        void stopLiveRecording_noTranscript_throwsException() {
            // given
            MockMultipartFile file =
                    validFile();

            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(
                            recordingSession(
                                    RecordingStatus.RECORDING
                            )
                    );

            when(liveTranscriptionService.finish(10L))
                    .thenReturn("   ");

            // when
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () ->
                                    recordingService
                                            .stopLiveRecording(
                                                    1L,
                                                    10L,
                                                    file
                                            )
                    );

            // then
            assertEquals(
                    ErrorCode.INVALID_REQUEST,
                    exception.getErrorCode()
            );

            verifyNoInteractions(recordingStorage);
            verifyNoInteractions(recordingAsyncService);
        }


        @Test
        @DisplayName("RECORDING 상태가 아니면 실시간 녹음을 종료할 수 없다")
        void stopLiveRecording_invalidStatus_throwsException() {
            // given
            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(
                            recordingSession(
                                    RecordingStatus.COMPLETED
                            )
                    );

            // when
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () ->
                                    recordingService
                                            .stopLiveRecording(
                                                    1L,
                                                    10L,
                                                    validFile()
                                            )
                    );

            // then
            assertEquals(
                    ErrorCode.INVALID_REQUEST,
                    exception.getErrorCode()
            );

            verifyNoInteractions(liveTranscriptionService);
        }
    }


    /*
     * =========================================================
     * 상태 조회
     * =========================================================
     */
    @Nested
    @DisplayName("녹음 상태 조회")
    class GetRecordingStatus {

        @Test
        @DisplayName("녹음 세션의 상태와 분석 결과를 조회한다")
        void getRecordingStatus_success() {
            // given
            RecordingSession session =
                    RecordingSession.builder()
                            .recordingSessionId(10L)
                            .accountId(1L)
                            .status(
                                    RecordingStatus.COMPLETED
                            )
                            .fullTranscript(
                                    "전체 녹취 내용"
                            )
                            .summary(
                                    "체크리스트 분석 완료"
                            )
                            .failureReason(null)
                            .build();

            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(session);

            // when
            RecordingStatusResponseDTO result =
                    recordingService
                            .getRecordingStatus(
                                    1L,
                                    10L
                            );

            // then
            assertEquals(
                    10L,
                    result.getRecordingSessionId()
            );

            assertEquals(
                    RecordingStatus.COMPLETED,
                    result.getStatus()
            );

            assertEquals(
                    "전체 녹취 내용",
                    result.getTranscript()
            );

            assertEquals(
                    "체크리스트 분석 완료",
                    result.getSummary()
            );
        }
    }


    /*
     * =========================================================
     * 녹취 조회
     * =========================================================
     */
    @Nested
    @DisplayName("녹취 조회")
    class GetTranscript {

        @Test
        @DisplayName("녹음 세션의 전체 녹취 내용을 조회한다")
        void getTranscript_success() {
            // given
            RecordingSession session =
                    RecordingSession.builder()
                            .recordingSessionId(10L)
                            .accountId(1L)
                            .fullTranscript(
                                    "전체 녹취 내용입니다."
                            )
                            .build();

            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(session);

            // when
            RecordingTranscriptResponseDTO result =
                    recordingService.getTranscript(
                            1L,
                            10L
                    );

            // then
            assertEquals(
                    10L,
                    result.getRecordingSessionId()
            );

            assertEquals(
                    "전체 녹취 내용입니다.",
                    result.getTranscript()
            );
        }


        @Test
        @DisplayName("존재하지 않는 녹음 세션은 조회할 수 없다")
        void getTranscript_notFound_throwsException() {
            // given
            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(null);

            // when
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () ->
                                    recordingService
                                            .getTranscript(
                                                    1L,
                                                    10L
                                            )
                    );

            // then
            assertEquals(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    exception.getErrorCode()
            );
        }
    }


    /*
     * =========================================================
     * 녹음 삭제
     * =========================================================
     */
    @Nested
    @DisplayName("녹음 삭제")
    class DeleteRecording {

        @Test
        @DisplayName("녹음 파일과 분석 결과, 세션, Context를 모두 삭제한다")
        void deleteRecording_success() {
            // given
            RecordingSession session =
                    RecordingSession.builder()
                            .recordingSessionId(10L)
                            .accountId(1L)
                            .status(
                                    RecordingStatus.COMPLETED
                            )
                            .storageObjectKey(
                                    "recordings/test.mp3"
                            )
                            .build();

            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(session);

            when(recordingSessionMapper
                    .deleteByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(1);

            // when
            recordingService.deleteRecording(
                    1L,
                    10L
            );

            // then
            verify(recordingStorage)
                    .delete(
                            "recordings/test.mp3"
                    );

            verify(recordingSessionMapper)
                    .deleteAnalysisResults(10L);

            verify(recordingSessionMapper)
                    .deleteByIdAndAccountId(
                            10L,
                            1L
                    );

            verify(contextManager)
                    .remove(10L);
        }


        @Test
        @DisplayName("실시간 녹음 중인 세션은 삭제할 수 없다")
        void deleteRecording_recording_throwsException() {
            // given
            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(
                            recordingSession(
                                    RecordingStatus.RECORDING
                            )
                    );

            // when
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () ->
                                    recordingService
                                            .deleteRecording(
                                                    1L,
                                                    10L
                                            )
                    );

            // then
            assertEquals(
                    ErrorCode.INVALID_REQUEST,
                    exception.getErrorCode()
            );

            verify(recordingSessionMapper, never())
                    .deleteByIdAndAccountId(
                            anyLong(),
                            anyLong()
                    );

            verifyNoInteractions(recordingStorage);
            verifyNoInteractions(contextManager);
        }


        @Test
        @DisplayName("Storage 파일이 없는 세션은 DB 데이터만 삭제한다")
        void deleteRecording_withoutFile_success() {
            // given
            RecordingSession session =
                    RecordingSession.builder()
                            .recordingSessionId(10L)
                            .accountId(1L)
                            .status(
                                    RecordingStatus.COMPLETED
                            )
                            .storageObjectKey(null)
                            .build();

            when(recordingSessionMapper
                    .findByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(session);

            when(recordingSessionMapper
                    .deleteByIdAndAccountId(
                            10L,
                            1L
                    ))
                    .thenReturn(1);

            // when
            recordingService.deleteRecording(
                    1L,
                    10L
            );

            // then
            verifyNoInteractions(recordingStorage);

            verify(recordingSessionMapper)
                    .deleteAnalysisResults(10L);

            verify(recordingSessionMapper)
                    .deleteByIdAndAccountId(
                            10L,
                            1L
                    );

            verify(contextManager)
                    .remove(10L);
        }
    }


    /*
     * =========================================================
     * 공통 테스트 데이터
     * =========================================================
     */

    private MockMultipartFile validFile() {
        return new MockMultipartFile(
                "file",
                "recording.mp3",
                "audio/mpeg",
                "audio-data".getBytes()
        );
    }

    private RecordingSession recordingSession(
            RecordingStatus status
    ) {
        return RecordingSession.builder()
                .recordingSessionId(10L)
                .accountId(1L)
                .reportChecklistId(100L)
                .status(status)
                .build();
    }
}