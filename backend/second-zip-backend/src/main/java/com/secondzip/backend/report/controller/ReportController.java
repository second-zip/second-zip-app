package com.secondzip.backend.report.controller;

import com.secondzip.backend.report.dto.request.CreateReportRequest;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.service.ReportService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags="분석 보고서 API")
@RestController
@RequestMapping("/api/analysis-reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/analyze")
    public ResponseEntity<ReportDetailResponse> analyze(
            @RequestBody CreateReportRequest request,
            @ApiIgnore Authentication authentication
    ) {
        Long accountId = Long.valueOf(authentication.getPrincipal().toString());
        ReportDetailResponse result = reportService.createReport(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}