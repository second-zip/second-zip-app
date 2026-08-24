package com.secondzip.backend.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.domain.AnalysisReport;
import com.secondzip.backend.report.domain.ReportCheckResult;
import com.secondzip.backend.report.domain.ReportFraudType;
import com.secondzip.backend.report.dto.*;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.FraudType;
import com.secondzip.backend.report.enums.RiskLevel;
import com.secondzip.backend.report.mapper.ReportMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ReportPersistenceServiceBranchTest {

    @Test
    @DisplayName("리포트와 점검·사기유형·상세·자동확인 결과를 하나의 저장 흐름으로 변환한다")
    void persistsCompleteEvaluationGraph() {
        ReportMapper mapper = mock(ReportMapper.class);
        ReportQueryService queryService = mock(ReportQueryService.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, AnalysisReport.class).setAnalysisReportId(99L);
            return null;
        }).when(mapper).insertReport(any(AnalysisReport.class));
        doAnswer(invocation -> {
            invocation.getArgument(0, ReportFraudType.class).setReportFraudTypeId(200L);
            return null;
        }).when(mapper).insertFraudType(any(ReportFraudType.class));
        ReportPersistenceService service = new ReportPersistenceService(
                mapper,
                new ObjectMapper(),
                queryService
        );

        CheckResultDTO hugCheck = new CheckResultDTO(
                CheckType.HUG_GUARANTEE_ELIGIBILITY,
                RiskLevel.SAFE,
                DataStatus.VERIFIED,
                Map.of("deposit", 100_000_000L)
        );
        DetailResultDTO ratioDetail = new DetailResultDTO(
                DetailType.HIGH_JEONSE_RATIO,
                RiskLevel.SAFE,
                DataStatus.VERIFIED
        );
        FraudTypeResultDTO fraud = new FraudTypeResultDTO(
                FraudType.UNDERWATER_JEONSE,
                RiskLevel.CAUTION,
                List.of(ratioDetail)
        );
        RiskEvaluationResultDTO evaluation = new RiskEvaluationResultDTO(
                RiskLevel.CAUTION,
                List.of(hugCheck),
                List.of(fraud)
        );

        ReportDetailResponse result = service.save(
                7L,
                "request-1",
                "서울특별시 강남구 테헤란로 1",
                "101동 1001호",
                100_000_000L,
                evaluation,
                "APARTMENT",
                false
        );

        assertEquals(99L, result.getAnalysisReportId());
        assertEquals(1, result.getCheckResults().size());
        assertEquals(1, result.getFraudTypes().size());
        assertFalse(result.getTrustProperty());

        ArgumentCaptor<ReportCheckResult> checkCaptor =
                ArgumentCaptor.forClass(ReportCheckResult.class);
        verify(mapper).insertCheckResult(checkCaptor.capture());
        assertEquals(
                DataStatus.VERIFIED.name(),
                checkCaptor.getValue().getDataStatus()
        );
        assertEquals(
                "{\"deposit\":100000000}",
                checkCaptor.getValue().getEvidence()
        );
        verify(mapper).insertDetailResult(
                200L,
                DetailType.HIGH_JEONSE_RATIO,
                RiskLevel.SAFE,
                DataStatus.VERIFIED
        );

        ArgumentCaptor<List<VerifiedChecklistItemDTO>> verifiedCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(mapper).insertChecklistVerifications(
                eq(99L),
                verifiedCaptor.capture()
        );
        List<VerifiedChecklistItemDTO> verified = verifiedCaptor.getValue();
        // HUG/HF/SGI는 각 보증기관 요건을 모두 확인할 수 없어 자동 체크하지 않고,
        // 가격 근거로 안전하게 확인된 전세가율만 자동 체크한다.
        assertEquals(1, verified.size());
        assertEquals(
                "전세가율 확인",
                verified.get(0).getContents()
        );
    }

    @Test
    @DisplayName("확인 완료 판정이 없으면 자동 체크 저장 쿼리를 호출하지 않는다")
    void skipsChecklistVerificationInsertWhenNothingWasVerified() {
        ReportMapper mapper = mock(ReportMapper.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, AnalysisReport.class).setAnalysisReportId(1L);
            return null;
        }).when(mapper).insertReport(any(AnalysisReport.class));
        ReportPersistenceService service = new ReportPersistenceService(
                mapper,
                new ObjectMapper(),
                mock(ReportQueryService.class)
        );

        service.save(
                7L,
                "request-1",
                "서울특별시 강남구 테헤란로 1",
                null,
                100_000_000L,
                new RiskEvaluationResultDTO(RiskLevel.SAFE, List.of(), List.of()),
                "APARTMENT",
                false
        );

        verify(mapper, never()).insertChecklistVerifications(any(), anyList());
    }

    @Test
    @DisplayName("삭제와 즐겨찾기 변경은 소유권을 먼저 확인하고 매퍼에 위임한다")
    void validatesOwnershipBeforeMutations() {
        ReportMapper mapper = mock(ReportMapper.class);
        ReportQueryService queryService = mock(ReportQueryService.class);
        ReportPersistenceService service = new ReportPersistenceService(
                mapper,
                new ObjectMapper(),
                queryService
        );

        service.deleteReport(7L, 77L);
        service.addFavorite(7L, 77L);
        service.removeFavorite(7L, 77L);

        verify(queryService, org.mockito.Mockito.times(3))
                .validateOwnership(7L, 77L);
        verify(mapper).deleteReport(77L);
        verify(mapper).updateFavorite(
                eq(77L),
                eq(true),
                any(LocalDateTime.class)
        );
        verify(mapper).updateFavorite(eq(77L), eq(false), isNull());
    }
}
