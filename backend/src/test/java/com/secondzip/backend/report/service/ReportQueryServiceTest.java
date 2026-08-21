package com.secondzip.backend.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.domain.AnalysisReport;
import com.secondzip.backend.report.domain.ReportCheckResult;
import com.secondzip.backend.report.domain.ReportFraudType;
import com.secondzip.backend.report.domain.ReportSpecialTerm;
import com.secondzip.backend.report.dto.DetailResult;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.dto.response.ReportListItem;
import com.secondzip.backend.report.dto.response.ReportListResponse;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.FraudType;
import com.secondzip.backend.report.enums.RiskLevel;
import com.secondzip.backend.report.mapper.ReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportQueryServiceTest {

    @Mock
    private ReportMapper reportMapper;

    private ReportQueryService service;

    @BeforeEach
    void setUp() {
        service = new ReportQueryService(reportMapper, new ObjectMapper());
    }

    @Test
    @DisplayName("리포트 목록과 전체 개수를 함께 반환한다")
    void getsReportListAndTotalCount() {
        List<ReportListItem> reports = List.of(new ReportListItem(
                10L,
                "서울특별시 강남구 테헤란로 1",
                "101호",
                RiskLevel.CAUTION,
                true,
                LocalDateTime.of(2026, 8, 1, 12, 0),
                LocalDateTime.of(2026, 7, 1, 12, 0)
        ));
        when(reportMapper.findReportsByAccountId(42L)).thenReturn(reports);
        when(reportMapper.countReportsByAccountId(42L)).thenReturn(7);

        ReportListResponse result = service.getReportList(42L);

        assertThat(result.getReports()).isSameAs(reports);
        assertThat(result.getTotalCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("요청 ID로 저장된 리포트 ID 조회를 위임한다")
    void findsReportIdByRequestId() {
        when(reportMapper.findReportIdByRequestId(42L, "request-id"))
                .thenReturn(10L);

        assertThat(service.findReportIdByRequestId(42L, "request-id"))
                .isEqualTo(10L);
    }

    @Test
    @DisplayName("리포트 소유 계정이 일치하면 소유권 검증을 통과한다")
    void validatesMatchingOwner() {
        when(reportMapper.findAccountIdByReportId(10L)).thenReturn(42L);

        assertDoesNotThrow(() -> service.validateOwnership(42L, 10L));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {7L})
    @DisplayName("리포트가 없거나 다른 계정 소유이면 동일한 404로 숨긴다")
    void rejectsMissingOrForeignReport(Long ownerId) {
        when(reportMapper.findAccountIdByReportId(10L)).thenReturn(ownerId);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validateOwnership(42L, 10L)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(exception).hasMessage("리포트를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("상세 조회는 JSON 근거, 데이터 상태, 사기 상세와 특약 순번을 조립한다")
    void buildsCompleteReportDetail() {
        when(reportMapper.findAccountIdByReportId(10L)).thenReturn(42L);

        AnalysisReport report = AnalysisReport.builder()
                .analysisReportId(10L)
                .roadAddress("서울특별시 강남구 테헤란로 1")
                .detailAddress("101호")
                .deposit(300_000_000L)
                .riskLevel(RiskLevel.DANGER)
                .favorite(true)
                .housingCategory(null)
                .trustProperty(false)
                .build();
        when(reportMapper.findReportById(10L)).thenReturn(report);

        when(reportMapper.findCheckResultsByReportId(10L)).thenReturn(List.of(
                ReportCheckResult.builder()
                        .checkType(CheckType.MORTGAGE_EXISTENCE)
                        .riskLevel(RiskLevel.CAUTION)
                        .dataStatus("UNVERIFIED")
                        .evidence("{\"mortgageAmount\":120000000,\"source\":\"CODEF\"}")
                        .build(),
                ReportCheckResult.builder()
                        .checkType(CheckType.ILLEGAL_BUILDING)
                        .riskLevel(RiskLevel.SAFE)
                        .dataStatus(null)
                        .evidence(null)
                        .build(),
                // data_status 는 V3 에서 추가된 컬럼이라, 모르는 값이 들어와도
                // 500 대신 VERIFIED 로 흘려보내는 방어가 걸려 있다.
                ReportCheckResult.builder()
                        .checkType(CheckType.BUILDING_USE)
                        .riskLevel(RiskLevel.CAUTION)
                        .dataStatus("UNKNOWN_LEGACY_VALUE")
                        .evidence("not-json")
                        .build()
        ));

        when(reportMapper.findFraudTypesByReportId(10L)).thenReturn(List.of(
                ReportFraudType.builder()
                        .reportFraudTypeId(20L)
                        .fraudType(FraudType.UNDERWATER_JEONSE)
                        .riskLevel(RiskLevel.DANGER)
                        .build()
        ));
        when(reportMapper.findDetailResultsByFraudTypeId(20L)).thenReturn(List.of(
                new DetailResult(
                        DetailType.HIGH_JEONSE_RATIO,
                        RiskLevel.DANGER,
                        DataStatus.VERIFIED
                ),
                new DetailResult(
                        DetailType.PRIORITY_DEBT_BURDEN,
                        RiskLevel.CAUTION,
                        null
                )
        ));
        when(reportMapper.findSpecialTermsByReportId(10L)).thenReturn(List.of(
                ReportSpecialTerm.builder().title("특약 A").content("내용 A").build(),
                ReportSpecialTerm.builder().title("특약 B").content("내용 B").build()
        ));

        ReportDetailResponse result = service.getReportDetail(42L, 10L);

        assertThat(result.getAnalysisReportId()).isEqualTo(10L);
        assertThat(result.getResult()).isEqualTo(RiskLevel.DANGER);
        assertThat(result.getHousingCategory()).isNull();
        assertThat(result.getTrustProperty()).isFalse();

        assertThat(result.getCheckResults()).hasSize(3);
        assertThat(result.getCheckResults().get(0).getCheckType())
                .isEqualTo(CheckType.MORTGAGE_EXISTENCE);
        assertThat(result.getCheckResults().get(0).getDataStatus())
                .isEqualTo(DataStatus.UNVERIFIED);
        assertThat(result.getCheckResults().get(0).getEvidence())
                .containsEntry("source", "CODEF")
                .containsEntry("mortgageAmount", 120_000_000);
        assertThat(result.getCheckResults().get(1).getEvidence()).isEmpty();
        assertThat(result.getCheckResults().get(1).getDataStatus())
                .isEqualTo(DataStatus.VERIFIED);
        assertThat(result.getCheckResults().get(2).getEvidence()).isEmpty();
        assertThat(result.getCheckResults().get(2).getDataStatus())
                .isEqualTo(DataStatus.VERIFIED);

        assertThat(result.getFraudTypes()).hasSize(1);
        assertThat(result.getFraudTypes().get(0).getFraudType())
                .isEqualTo(FraudType.UNDERWATER_JEONSE);
        assertThat(result.getFraudTypes().get(0).getDetailResults())
                .hasSize(2);
        assertThat(result.getFraudTypes().get(0).getDetailResults().get(1)
                .getDataStatus()).isEqualTo(DataStatus.VERIFIED);

        assertThat(result.getSpecialTerms())
                .extracting(term -> term.getSequence())
                .containsExactly(1, 2);
        assertThat(result.getSpecialTerms())
                .extracting(term -> term.getTitle())
                .containsExactly("특약 A", "특약 B");
    }

    @Test
    @DisplayName("소유권 확인 직후 리포트가 삭제된 경쟁 상황도 404로 반환한다")
    void returnsNotFoundWhenReportDisappearsAfterOwnershipCheck() {
        when(reportMapper.findAccountIdByReportId(10L)).thenReturn(42L);
        when(reportMapper.findReportById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getReportDetail(42L, 10L)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verify(reportMapper, never()).findCheckResultsByReportId(10L);
        verify(reportMapper, never()).findFraudTypesByReportId(10L);
        verify(reportMapper, never()).findSpecialTermsByReportId(10L);
    }
}
