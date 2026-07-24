package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.response.ReportListItem;
import com.secondzip.backend.report.dto.response.ReportListResponse;
import com.secondzip.backend.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportQueryService {

    private final ReportMapper reportMapper;

    // 리포트 담당
    // 리포트 목록 불러오기 (조회)
    public ReportListResponse getReportList(Long accountId) {
        List<ReportListItem> reports = reportMapper.findReportsByAccountId(accountId);
        int totalCount = reportMapper.countReportsByAccountId(accountId);
        boolean hasMore = totalCount > reports.size();
        return new ReportListResponse(reports, totalCount, hasMore);
    }
}