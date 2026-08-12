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

    // 즐겨찾기 추가
    public void addFavorite(Long accountId, Long reportId) {
        reportPersistenceService.addFavorite(accountId, reportId);
    }

    // 즐겨찾기 해제
    public void removeFavorite(Long accountId, Long reportId) {
        reportPersistenceService.removeFavorite(accountId, reportId);
    }
}
