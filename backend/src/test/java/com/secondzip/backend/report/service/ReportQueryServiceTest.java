package com.secondzip.backend.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        Map<String, Object> report = new HashMap<>();
        report.put("analysisReportId", 10L);
        report.put("roadAddress", "서울특별시 강남구 테헤란로 1");
        report.put("detailAddress", "101호");
        report.put("deposit", 300_000_000L);
        report.put("result", "DANGER");
        report.put("favorite", true);
        report.put("housingCategory", null);
        report.put("trustProperty", false);
        when(reportMapper.findReportById(10L)).thenReturn(report);

        when(reportMapper.findCheckResultsByReportId(10L)).thenReturn(List.of(
                row(
                        "checkType", "MORTGAGE_EXISTENCE",
                        "riskLevel", "CAUTION",
                        "dataStatus", "UNVERIFIED",
                        "evidence", "{\"mortgageAmount\":120000000,\"source\":\"CODEF\"}"
                ),
                row(
                        "checkType", "ILLEGAL_BUILDING",
                        "riskLevel", "SAFE",
                        "dataStatus", null,
                        "evidence", null
                ),
                row(
                        "checkType", "BUILDING_USE",
                        "riskLevel", "CAUTION",
                        "dataStatus", "UNKNOWN_LEGACY_VALUE",
                        "evidence", "not-json"
                )
        ));

        when(reportMapper.findFraudTypesByReportId(10L)).thenReturn(List.of(
                row(
                        "reportFraudTypeId", 20L,
                        "fraudType", "UNDERWATER_JEONSE",
                        "riskLevel", "DANGER"
                )
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
                Map.of("title", "특약 A", "content", "내용 A"),
                Map.of("title", "특약 B", "content", "내용 B")
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

    private Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new HashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            row.put((String) keyValues[index], keyValues[index + 1]);
        }
        return row;
    }
}
