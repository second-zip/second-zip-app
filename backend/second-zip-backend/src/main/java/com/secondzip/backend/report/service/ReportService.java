package com.secondzip.backend.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportPersistenceService reportPersistenceService;

    // 리포트 삭제
    public void deleteReport(Long accountId, Long reportId) {
        reportPersistenceService.deleteReport(accountId, reportId);
    }
}
