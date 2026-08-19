package com.secondzip.backend.report.controller;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.report.dto.request.ContinueAnalysisAuthRequest;
import com.secondzip.backend.report.dto.request.CreateReportRequest;
import com.secondzip.backend.report.dto.request.StartAnalysisAuthRequest;
import com.secondzip.backend.report.service.ReportQueryService;
import com.secondzip.backend.report.service.ReportService;
import com.secondzip.backend.report.service.ReportShareService;
import com.secondzip.backend.report.service.SpecialTermService;
import com.secondzip.backend.report.service.workflow.AnalysisAuthenticationService;
import com.secondzip.backend.report.service.workflow.AnalysisExecutionService;
import com.secondzip.backend.report.service.workflow.AnalysisPreparationService;
import com.secondzip.backend.report.service.workflow.ExternalApiReadinessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportControllerTest {

    private ReportService reportService;
    private ReportQueryService reportQueryService;
    private AnalysisPreparationService analysisPreparationService;
    private AnalysisAuthenticationService analysisAuthenticationService;
    private AnalysisExecutionService analysisExecutionService;
    private ExternalApiReadinessService externalApiReadinessService;
    private SpecialTermService specialTermService;
    private ReportShareService reportShareService;
    private ReportController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        reportService = mock(ReportService.class);
        reportQueryService = mock(ReportQueryService.class);
        analysisPreparationService = mock(AnalysisPreparationService.class);
        analysisAuthenticationService = mock(AnalysisAuthenticationService.class);
        analysisExecutionService = mock(AnalysisExecutionService.class);
        externalApiReadinessService = mock(ExternalApiReadinessService.class);
        specialTermService = mock(SpecialTermService.class);
        reportShareService = mock(ReportShareService.class);
        controller = new ReportController(
                reportService,
                reportQueryService,
                analysisPreparationService,
                analysisAuthenticationService,
                analysisExecutionService,
                externalApiReadinessService,
                specialTermService,
                reportShareService
        );
        authentication = authenticated(7L);
    }

    @Test
    @DisplayName("단계형 분석 엔드포인트는 인증 계정과 요청을 각 서비스에 전달한다")
    void delegatesAnalysisWorkflowEndpoints() {
        CreateReportRequest createRequest = mock(CreateReportRequest.class);
        StartAnalysisAuthRequest startRequest = mock(StartAnalysisAuthRequest.class);
        ContinueAnalysisAuthRequest continueRequest = mock(ContinueAnalysisAuthRequest.class);

        assertEquals(
                HttpStatus.CREATED,
                controller.prepareAnalysis(createRequest, authentication).getStatusCode()
        );
        assertEquals(
                HttpStatus.OK,
                controller.getAnalysisRequest("request-1", authentication).getStatusCode()
        );
        assertEquals(
                HttpStatus.OK,
                controller.externalReadiness(authentication).getStatusCode()
        );
        assertEquals(
                HttpStatus.OK,
                controller.startAnalysisAuthentication(
                        "request-1",
                        startRequest,
                        authentication
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.OK,
                controller.continueAnalysisAuthentication(
                        "request-1",
                        continueRequest,
                        authentication
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.CREATED,
                controller.completeAnalysis("request-1", authentication).getStatusCode()
        );
        assertEquals(
                HttpStatus.OK,
                controller.retryAnalysis("request-1", authentication).getStatusCode()
        );

        verify(analysisPreparationService).prepare(7L, createRequest);
        verify(analysisPreparationService).getStatus(7L, "request-1");
        verify(externalApiReadinessService).check();
        verify(analysisAuthenticationService).start(7L, "request-1", startRequest);
        verify(analysisAuthenticationService)
                .continueAuthentication(7L, "request-1", continueRequest);
        verify(analysisExecutionService).execute(7L, "request-1");
        verify(analysisExecutionService).retry(7L, "request-1");
    }

    @Test
    @DisplayName("리포트 조회·수정 엔드포인트는 소유권 계정과 리포트 ID를 전달한다")
    void delegatesReportEndpoints() {
        when(specialTermService.generateAndSave(7L, 10L))
                .thenReturn(List.of());

        assertEquals(HttpStatus.OK, controller.getList(authentication).getStatusCode());
        assertEquals(
                HttpStatus.OK,
                controller.getDetail(10L, authentication).getStatusCode()
        );
        assertEquals(
                HttpStatus.NO_CONTENT,
                controller.deleteReport(10L, authentication).getStatusCode()
        );
        assertEquals(
                HttpStatus.OK,
                controller.generateSpecialTerms(10L, authentication).getStatusCode()
        );
        assertEquals(
                HttpStatus.NO_CONTENT,
                controller.addFavorite(10L, authentication).getStatusCode()
        );
        assertEquals(
                HttpStatus.NO_CONTENT,
                controller.removeFavorite(10L, authentication).getStatusCode()
        );
        assertEquals(
                HttpStatus.CREATED,
                controller.createShareLink(10L, authentication).getStatusCode()
        );
        assertEquals(
                HttpStatus.OK,
                controller.getSharedReport("share-token").getStatusCode()
        );

        verify(reportQueryService).getReportList(7L);
        verify(reportQueryService).getReportDetail(7L, 10L);
        verify(reportQueryService).validateOwnership(7L, 10L);
        verify(reportService).deleteReport(7L, 10L);
        verify(specialTermService).generateAndSave(7L, 10L);
        verify(reportService).addFavorite(7L, 10L);
        verify(reportService).removeFavorite(7L, 10L);
        verify(reportShareService).createShareLink(7L, 10L);
        verify(reportShareService).getSharedReport("share-token");
    }

    @Test
    @DisplayName("인증 정보가 없거나 익명인 요청은 서비스 호출 전에 거절한다")
    void rejectsUnauthenticatedAndAnonymousRequests() {
        Authentication unauthenticated = mock(Authentication.class);
        Authentication anonymous = mock(Authentication.class);
        when(anonymous.isAuthenticated()).thenReturn(true);
        when(anonymous.getPrincipal()).thenReturn("anonymousUser");

        assertThrows(
                BusinessException.class,
                () -> controller.getList(null)
        );
        assertThrows(
                BusinessException.class,
                () -> controller.getList(unauthenticated)
        );
        assertThrows(
                BusinessException.class,
                () -> controller.getList(anonymous)
        );
    }

    private Authentication authenticated(Long accountId) {
        Authentication result = mock(Authentication.class);
        when(result.isAuthenticated()).thenReturn(true);
        when(result.getPrincipal()).thenReturn(accountId);
        return result;
    }
}
