package com.secondzip.backend.report.controller;

import com.secondzip.backend.report.dto.request.CreateReportRequest;
import com.secondzip.backend.report.dto.request.StartAnalysisAuthRequest;
import com.secondzip.backend.report.dto.request.ContinueAnalysisAuthRequest;
import com.secondzip.backend.report.dto.response.AnalysisAuthResponse;
import com.secondzip.backend.report.dto.response.AnalysisPreparationResponse;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.dto.response.ReportListResponse;
import com.secondzip.backend.report.dto.response.ExternalApiReadinessResponse;
import com.secondzip.backend.report.service.ReportQueryService;
import com.secondzip.backend.report.service.ReportService;
import com.secondzip.backend.report.service.workflow.AnalysisPreparationService;
import com.secondzip.backend.report.service.workflow.AnalysisAuthenticationService;
import com.secondzip.backend.report.service.workflow.AnalysisExecutionService;
import com.secondzip.backend.report.service.workflow.ExternalApiReadinessService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;

@Api(tags="분석 보고서 API", description = "분석 보고서 생성 기능을 제공합니다.")
@RestController
@RequestMapping("/api/analysis-reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportQueryService reportQueryService;
    private final AnalysisPreparationService analysisPreparationService;
    private final AnalysisAuthenticationService analysisAuthenticationService;
    private final AnalysisExecutionService analysisExecutionService;
    private final ExternalApiReadinessService externalApiReadinessService;

    @PostMapping("/requests")
    @ApiOperation(
            value = "단계형 전세 위험 분석 준비",
            notes = "주소와 건축물 유형을 확인하고 CODEF 인증 전 분석 요청을 생성합니다."
    )
    public ResponseEntity<AnalysisPreparationResponse> prepareAnalysis(
            @Valid @RequestBody CreateReportRequest request,
            @ApiIgnore Authentication authentication
    ) {
        Long accountId = authenticatedAccountId(authentication);
        AnalysisPreparationResponse response =
                analysisPreparationService.prepare(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/requests/{requestId}")
    @ApiOperation(value = "단계형 분석 요청 상태 조회")
    public ResponseEntity<AnalysisPreparationResponse> getAnalysisRequest(
            @PathVariable String requestId,
            @ApiIgnore Authentication authentication
    ) {
        Long accountId = authenticatedAccountId(authentication);
        return ResponseEntity.ok(
                analysisPreparationService.getStatus(accountId, requestId)
        );
    }

    @GetMapping("/external-readiness")
    @ApiOperation(value = "외부 API 데모 설정 준비상태 확인")
    public ResponseEntity<ExternalApiReadinessResponse> externalReadiness(
            @ApiIgnore Authentication authentication
    ) {
        authenticatedAccountId(authentication);
        return ResponseEntity.ok(externalApiReadinessService.check());
    }

    @PostMapping("/requests/{requestId}/auth/start")
    @ApiOperation(value = "건축물대장 간편인증 시작")
    public ResponseEntity<AnalysisAuthResponse> startAnalysisAuthentication(
            @PathVariable String requestId,
            @Valid @RequestBody StartAnalysisAuthRequest request,
            @ApiIgnore Authentication authentication
    ) {
        Long accountId = authenticatedAccountId(authentication);
        return ResponseEntity.ok(
                analysisAuthenticationService.start(accountId, requestId, request)
        );
    }

    @PostMapping("/requests/{requestId}/auth/continue")
    @ApiOperation(value = "건축물대장 간편인증·주소·동·호 선택 계속")
    public ResponseEntity<AnalysisAuthResponse> continueAnalysisAuthentication(
            @PathVariable String requestId,
            @Valid @RequestBody ContinueAnalysisAuthRequest request,
            @ApiIgnore Authentication authentication
    ) {
        Long accountId = authenticatedAccountId(authentication);
        return ResponseEntity.ok(
                analysisAuthenticationService.continueAuthentication(
                        accountId,
                        requestId,
                        request
                )
        );
    }

    @PostMapping("/requests/{requestId}/complete")
    @ApiOperation(value = "인증 완료 데이터로 최종 분석 및 리포트 생성")
    public ResponseEntity<ReportDetailResponse> completeAnalysis(
            @PathVariable String requestId,
            @ApiIgnore Authentication authentication
    ) {
        Long accountId = authenticatedAccountId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                analysisExecutionService.execute(accountId, requestId)
        );
    }

    @PostMapping("/requests/{requestId}/retry")
    @ApiOperation(value = "실패한 최종 분석 재시도")
    public ResponseEntity<ReportDetailResponse> retryAnalysis(
            @PathVariable String requestId,
            @ApiIgnore Authentication authentication
    ) {
        Long accountId = authenticatedAccountId(authentication);
        return ResponseEntity.ok(
                analysisExecutionService.retry(accountId, requestId)
        );
    }

    // 리포트 목록 조회
    @GetMapping
    @ApiOperation(value = "분석 보고서 목록 조회", notes = "분석 보고서 목록을 조회합니다.")
    public ResponseEntity<ReportListResponse> getList(@ApiIgnore Authentication authentication) {
        Long accountId = authenticatedAccountId(authentication);
        ReportListResponse result = reportQueryService.getReportList(accountId);
        return ResponseEntity.ok(result);
    }

    // 리포트 상세 조회
    @GetMapping("/{analysisReportId}")
    @ApiOperation(value = "분석 보고서 상세 조회", notes = "분석 보고서의 상세 정보를 조회합니다.")
    public ResponseEntity<ReportDetailResponse> getDetail(
            @ApiParam(value = "리포트 ID", required = true)
            @PathVariable Long analysisReportId,
            @ApiIgnore Authentication authentication
    ) {
        Long accountId = authenticatedAccountId(authentication);
        ReportDetailResponse result = reportQueryService.getReportDetail(accountId, analysisReportId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{analysisReportId}")
    @ApiOperation(value = "분석 보고서 삭제", notes = "분석 보고서를 삭제합니다.")
    public ResponseEntity<Void> deleteReport(
            @ApiParam(value = "리포트 ID", required = true)
            @PathVariable Long analysisReportId,
            @ApiIgnore Authentication authentication
    ) {
        Long accountId = authenticatedAccountId(authentication);
        reportQueryService.validateOwnership(accountId, analysisReportId);
        reportService.deleteReport(accountId, analysisReportId);
        return ResponseEntity.noContent().build();

    }

    @PostMapping("/{analysisReportId}/favorite")
    @ApiOperation(value = "리포트 즐겨찾기 추가", notes = "분석 보고서를 즐겨찾기에 추가합니다.")
    public ResponseEntity<Void> addFavorite(
            @ApiParam(value = "리포트 ID", required = true)
            @PathVariable Long analysisReportId,
            @ApiIgnore Authentication authentication
    ) {
        Long accountId = authenticatedAccountId(authentication);
        reportService.addFavorite(accountId, analysisReportId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{analysisReportId}/favorite")
    @ApiOperation(value = "리포트 즐겨찾기 해제", notes = "분석 보고서의 즐겨찾기를 해제합니다.")
    public ResponseEntity<Void> removeFavorite(
            @ApiParam(value = "리포트 ID", required = true)
            @PathVariable Long analysisReportId,
            @ApiIgnore Authentication authentication
    ) {
        Long accountId = authenticatedAccountId(authentication);
        reportService.removeFavorite(accountId, analysisReportId);
        return ResponseEntity.noContent().build();
    }

    private Long authenticatedAccountId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new com.secondzip.backend.common.exception.BusinessException(
                    com.secondzip.backend.common.exception.ErrorCode.UNAUTHORIZED
            );
        }
        return (Long) authentication.getPrincipal();
    }
}
