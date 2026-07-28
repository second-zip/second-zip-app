package com.secondzip.backend.report.controller;

import com.secondzip.backend.report.dto.request.CreateReportRequest;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.dto.response.ReportListResponse;
import com.secondzip.backend.report.service.ReportQueryService;
import com.secondzip.backend.report.service.ReportService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags="분석 보고서 API", description = "분석 보고서 생성 기능을 제공합니다.")
@RestController
@RequestMapping("/api/analysis-reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportQueryService reportQueryService;

    // 분석
    @PostMapping("/analyze")
    @ApiOperation(value = "전세 위험도 분석 보고서 생성", notes = "주소와 보증금 정보를 받아 전세 위험도를 분석하고 상세 보고서를 생성합니다.")
//    @ApiResponses({
//            @ApiResponse(code=200, message = "분석 성공"),
//            @ApiResponse(code=400, message = "요청 값이 올바르지 않음"),
//            @ApiResponse(code=401, message = "이메일 또는 비밀번호가 일치하지 않음")
//    })
    public ResponseEntity<ReportDetailResponse> analyze(
            @ApiParam(value = "분석 요청 정보(주소, 보증금)", required = true) @RequestBody CreateReportRequest request,
            @ApiIgnore Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long accountId = (Long) authentication.getPrincipal();
        ReportDetailResponse result = reportService.createReport(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // 리포트 목록 조회
    @GetMapping
    @ApiOperation(value = "분석 보고서 목록 조회", notes = "분석 보고서 목록을 조회합니다.")
    public ResponseEntity<ReportListResponse> getList(@ApiIgnore Authentication authentication) {
        Long accountId = (Long) authentication.getPrincipal();
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
        Long accountId = (Long) authentication.getPrincipal();
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
        Long accountId = (Long) authentication.getPrincipal();
        reportQueryService.validateOwnership(accountId, analysisReportId);
        reportService.deleteReport(accountId, analysisReportId);
        return ResponseEntity.noContent().build();

    }
}