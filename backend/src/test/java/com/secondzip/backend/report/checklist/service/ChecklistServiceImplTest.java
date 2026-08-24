package com.secondzip.backend.report.checklist.service;

import com.secondzip.backend.checklist.domain.ReportChecklist;
import com.secondzip.backend.checklist.dto.request.ChecklistCheckRequestDTO;
import com.secondzip.backend.checklist.dto.response.*;
import com.secondzip.backend.checklist.enums.Category;
import com.secondzip.backend.checklist.mapper.ReportChecklistMapper;
import com.secondzip.backend.checklist.service.ChecklistServiceImpl;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.record.domain.RecordingSession;
import com.secondzip.backend.record.mapper.RecordingSessionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistServiceImplTest {

    @Mock
    private ReportChecklistMapper reportChecklistMapper;

    @InjectMocks
    private ChecklistServiceImpl checklistService;

    @Mock
    private RecordingSessionMapper recordingSessionMapper;


    /*
     * =========================================================
     * 체크리스트 목록 조회
     * =========================================================
     */
    @Nested
    @DisplayName("체크리스트 목록 조회")
    class GetChecklistList {

        @Test
        @DisplayName("회원의 체크리스트 목록을 조회한다")
        void getChecklistList_success() {
            // given
            Long accountId = 1L;

            ChecklistListResponseDTO checklist1 =
                    mock(ChecklistListResponseDTO.class);

            ChecklistListResponseDTO checklist2 =
                    mock(ChecklistListResponseDTO.class);

            List<ChecklistListResponseDTO> expected =
                    List.of(checklist1, checklist2);

            when(reportChecklistMapper.findChecklistList(accountId))
                    .thenReturn(expected);

            // when
            List<ChecklistListResponseDTO> result =
                    checklistService.getChecklistList(accountId);

            // then
            assertEquals(expected, result);
            assertEquals(2, result.size());

            verify(reportChecklistMapper)
                    .findChecklistList(accountId);
        }

        @Test
        @DisplayName("체크리스트가 없으면 빈 목록을 반환한다")
        void getChecklistList_empty_returnsEmptyList() {
            // given
            when(reportChecklistMapper.findChecklistList(1L))
                    .thenReturn(List.of());

            // when
            List<ChecklistListResponseDTO> result =
                    checklistService.getChecklistList(1L);

            // then
            assertTrue(result.isEmpty());
        }
    }


    /*
     * =========================================================
     * 체크리스트 생성
     * =========================================================
     */
    @Nested
    @DisplayName("체크리스트 생성")
    class CreateChecklist {

        @Test
        @DisplayName("분석이 완료된 리포트이면 체크리스트를 생성한다")
        void createChecklist_success() {
            // given
            Long accountId = 1L;
            Long analysisReportId = 10L;
            Long reportChecklistId = 100L;

            ReportChecklistConditionDTO condition =
                    mock(ReportChecklistConditionDTO.class);

            when(condition.getHousingCategory())
                    .thenReturn(Category.APARTMENT);

            when(condition.getTrustProperty())
                    .thenReturn(false);

            when(reportChecklistMapper.findReportCondition(
                    analysisReportId,
                    accountId
            )).thenReturn(condition);

            when(reportChecklistMapper.findChecklistIdByReportId(
                    analysisReportId
            )).thenReturn(null);

            /*
             * 실제 MyBatis에서는 useGeneratedKeys로
             * reportChecklistId가 VO에 들어간다.
             * Mock 테스트에서도 같은 상황을 만들어준다.
             */
            when(reportChecklistMapper.insertChecklist(
                    any(ReportChecklist.class)
            )).thenAnswer(invocation -> {

                ReportChecklist checklist =
                        invocation.getArgument(0);

                ReflectionTestUtils.setField(
                        checklist,
                        "reportChecklistId",
                        reportChecklistId
                );

                return 1;
            });

            // when
            Long result = checklistService.createChecklist(
                    accountId,
                    analysisReportId
            );

            // then
            assertEquals(reportChecklistId, result);

            ArgumentCaptor<ReportChecklist> captor =
                    ArgumentCaptor.forClass(
                            ReportChecklist.class
                    );

            verify(reportChecklistMapper)
                    .insertChecklist(captor.capture());

            ReportChecklist saved =
                    captor.getValue();

            assertEquals(
                    analysisReportId,
                    saved.getAnalysisReportId()
            );

            assertEquals(
                    accountId,
                    saved.getAccountId()
            );

            verify(reportChecklistMapper)
                    .insertChecklistItems(
                            reportChecklistId,
                            analysisReportId,
                            Category.APARTMENT,
                            false
                    );
        }

        @Test
        @DisplayName("신탁 부동산이면 신탁 여부를 포함해 체크리스트 항목을 생성한다")
        void createChecklist_trustProperty_success() {
            // given
            ReportChecklistConditionDTO condition =
                    mock(ReportChecklistConditionDTO.class);

            when(condition.getHousingCategory())
                    .thenReturn(Category.APARTMENT);

            when(condition.getTrustProperty())
                    .thenReturn(true);

            when(reportChecklistMapper.findReportCondition(
                    10L,
                    1L
            )).thenReturn(condition);

            when(reportChecklistMapper
                    .findChecklistIdByReportId(10L))
                    .thenReturn(null);

            when(reportChecklistMapper.insertChecklist(any()))
                    .thenAnswer(invocation -> {

                        ReportChecklist checklist =
                                invocation.getArgument(0);

                        ReflectionTestUtils.setField(
                                checklist,
                                "reportChecklistId",
                                100L
                        );

                        return 1;
                    });

            // when
            checklistService.createChecklist(1L, 10L);

            // then
            verify(reportChecklistMapper)
                    .insertChecklistItems(
                            100L,
                            10L,
                            Category.APARTMENT,
                            true
                    );
        }

        @Test
        @DisplayName("리포트가 없거나 분석 조건을 조회할 수 없으면 생성에 실패한다")
        void createChecklist_conditionNotFound_throwsException() {
            // given
            when(reportChecklistMapper.findReportCondition(
                    10L,
                    1L
            )).thenReturn(null);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> checklistService
                            .createChecklist(1L, 10L)
            );

            // then
            assertEquals(
                    ErrorCode.INVALID_REQUEST,
                    exception.getErrorCode()
            );

            verify(reportChecklistMapper, never())
                    .insertChecklist(any());
        }

        @Test
        @DisplayName("주택 유형이 정해지지 않은 리포트이면 생성에 실패한다")
        void createChecklist_noHousingCategory_throwsException() {
            // given
            ReportChecklistConditionDTO condition =
                    mock(ReportChecklistConditionDTO.class);

            when(condition.getHousingCategory())
                    .thenReturn(null);

            when(reportChecklistMapper.findReportCondition(
                    10L,
                    1L
            )).thenReturn(condition);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> checklistService
                            .createChecklist(1L, 10L)
            );

            // then
            assertEquals(
                    ErrorCode.INVALID_REQUEST,
                    exception.getErrorCode()
            );

            verify(reportChecklistMapper, never())
                    .insertChecklist(any());
        }
    }


    /*
     * =========================================================
     * 중복 체크리스트 생성
     * =========================================================
     */
    @Nested
    @DisplayName("중복 체크리스트 생성")
    class DuplicateCreateChecklist {

        @Test
        @DisplayName("이미 생성된 체크리스트가 있으면 기존 ID를 반환한다")
        void createChecklist_alreadyExists_returnsExistingId() {
            // given
            ReportChecklistConditionDTO condition =
                    mock(ReportChecklistConditionDTO.class);

            when(condition.getHousingCategory())
                    .thenReturn(Category.APARTMENT);

            when(reportChecklistMapper.findReportCondition(
                    10L,
                    1L
            )).thenReturn(condition);

            when(reportChecklistMapper
                    .findChecklistIdByReportId(10L))
                    .thenReturn(100L);

            // when
            Long result =
                    checklistService.createChecklist(1L, 10L);

            // then
            assertEquals(100L, result);

            verify(reportChecklistMapper, never())
                    .insertChecklist(any());

            verify(reportChecklistMapper, never())
                    .insertChecklistItems(
                            anyLong(),
                            anyLong(),
                            any(),
                            anyBoolean()
                    );
        }

        @Test
        @DisplayName("동시 생성으로 UNIQUE 충돌이 발생하면 기존 체크리스트 ID를 반환한다")
        void createChecklist_duplicateKey_returnsExistingId() {
            // given
            ReportChecklistConditionDTO condition =
                    mock(ReportChecklistConditionDTO.class);

            when(condition.getHousingCategory())
                    .thenReturn(Category.APARTMENT);

            when(reportChecklistMapper.findReportCondition(
                    10L,
                    1L
            )).thenReturn(condition);

            /*
             * 첫 조회에서는 아직 다른 요청이 INSERT하기 전
             * → null
             *
             * DuplicateKey 발생 후 다시 조회
             * → 100L
             */
            when(reportChecklistMapper
                    .findChecklistIdByReportId(10L))
                    .thenReturn(null, 100L);

            when(reportChecklistMapper.insertChecklist(any()))
                    .thenThrow(
                            new DuplicateKeyException(
                                    "duplicate checklist"
                            )
                    );

            // when
            Long result =
                    checklistService.createChecklist(1L, 10L);

            // then
            assertEquals(100L, result);

            verify(reportChecklistMapper, times(2))
                    .findChecklistIdByReportId(10L);

            verify(reportChecklistMapper, never())
                    .insertChecklistItems(
                            anyLong(),
                            anyLong(),
                            any(),
                            anyBoolean()
                    );
        }
    }


    /*
     * =========================================================
     * 체크리스트 상세조회
     * =========================================================
     */
    @Nested
    @DisplayName("체크리스트 상세조회")
    class GetChecklist {

        @Test
        @DisplayName("체크리스트 주소와 항목을 조회한다")
        void getChecklist_success() {
            // given
            ChecklistAddressDTO address =
                    mock(ChecklistAddressDTO.class);

            when(address.getRoadAddress())
                    .thenReturn("서울특별시 강남구 테헤란로");

            when(address.getDetailAddress())
                    .thenReturn("101동 101호");

            ChecklistResponseDTO item1 =
                    mock(ChecklistResponseDTO.class);

            ChecklistResponseDTO item2 =
                    mock(ChecklistResponseDTO.class);

            List<ChecklistResponseDTO> items =
                    List.of(item1, item2);

            when(reportChecklistMapper.findChecklistAddress(
                    1L,
                    100L
            )).thenReturn(address);

            when(reportChecklistMapper.findChecklistItems(
                    1L,
                    100L
            )).thenReturn(items);


            // 새로 추가된 부분
            RecordingSession recordingSession =
                    RecordingSession.builder()
                            .recordingSessionId(200L)
                            .reportChecklistId(100L)
                            .accountId(1L)
                            .build();

            when(recordingSessionMapper
                    .findByReportChecklistId(100L))
                    .thenReturn(recordingSession);


            // when
            ChecklistDetailResponseDTO result =
                    checklistService.getChecklist(
                            1L,
                            100L
                    );

            // then
            assertEquals(
                    "서울특별시 강남구 테헤란로",
                    result.getRoadAddress()
            );

            assertEquals(
                    "101동 101호",
                    result.getDetailAddress()
            );

            assertEquals(items, result.getItems());

            // 새로 추가
            assertEquals(
                    200L,
                    result.getRecordingSessionId()
            );
        }

        @Test
        @DisplayName("본인의 체크리스트가 아니거나 존재하지 않으면 조회에 실패한다")
        void getChecklist_notFound_throwsException() {
            // given
            when(reportChecklistMapper.findChecklistAddress(
                    1L,
                    100L
            )).thenReturn(null);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> checklistService
                            .getChecklist(1L, 100L)
            );

            // then
            assertEquals(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    exception.getErrorCode()
            );

            verify(reportChecklistMapper, never())
                    .findChecklistItems(
                            anyLong(),
                            anyLong()
                    );
        }
    }


    /*
     * =========================================================
     * 체크 상태 변경
     * =========================================================
     */
    @Nested
    @DisplayName("체크 상태 변경")
    class UpdateCheckStatus {

        @Test
        @DisplayName("체크리스트 항목의 체크 상태를 변경한다")
        void updateCheckStatus_success() {
            // given
            ChecklistCheckRequestDTO request =
                    mock(ChecklistCheckRequestDTO.class);

            when(request.getChecked())
                    .thenReturn(true);

            when(reportChecklistMapper.updateChecked(
                    1L,
                    100L,
                    1000L,
                    true
            )).thenReturn(1);

            // when
            checklistService.updateCheckStatus(
                    1L,
                    100L,
                    1000L,
                    request
            );

            // then
            verify(reportChecklistMapper)
                    .updateChecked(
                            1L,
                            100L,
                            1000L,
                            true
                    );
        }

        @Test
        @DisplayName("체크리스트 항목을 찾을 수 없으면 상태 변경에 실패한다")
        void updateCheckStatus_notFound_throwsException() {
            // given
            ChecklistCheckRequestDTO request =
                    mock(ChecklistCheckRequestDTO.class);

            when(request.getChecked())
                    .thenReturn(true);

            when(reportChecklistMapper.updateChecked(
                    1L,
                    100L,
                    1000L,
                    true
            )).thenReturn(0);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> checklistService.updateCheckStatus(
                            1L,
                            100L,
                            1000L,
                            request
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
     * 체크리스트 초기화
     * =========================================================
     */
    @Nested
    @DisplayName("체크리스트 초기화")
    class ResetChecklist {

        @Test
        @DisplayName("본인의 체크리스트이면 모든 체크 상태를 초기화한다")
        void resetChecklist_success() {
            // given
            when(reportChecklistMapper.existsOwnedChecklist(
                    1L,
                    100L
            )).thenReturn(1);

            when(reportChecklistMapper.reset(
                    1L,
                    100L
            )).thenReturn(5);

            // when
            checklistService.resetChecklist(
                    1L,
                    100L
            );

            // then
            verify(reportChecklistMapper)
                    .existsOwnedChecklist(
                            1L,
                            100L
                    );

            verify(reportChecklistMapper, times(1))
                    .reset(
                            1L,
                            100L
                    );
        }

        @Test
        @DisplayName("본인의 체크리스트가 아니거나 존재하지 않으면 초기화에 실패한다")
        void resetChecklist_notFound_throwsException() {
            // given
            when(reportChecklistMapper.existsOwnedChecklist(
                    1L,
                    100L
            )).thenReturn(0);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> checklistService.resetChecklist(
                            1L,
                            100L
                    )
            );

            // then
            assertEquals(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    exception.getErrorCode()
            );

            verify(reportChecklistMapper, never())
                    .reset(anyLong(), anyLong());
        }
    }
}