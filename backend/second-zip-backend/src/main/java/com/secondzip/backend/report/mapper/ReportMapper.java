package com.secondzip.backend.report.mapper;

import com.secondzip.backend.report.dto.response.ReportListItem;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {
    void insertReportMap(Map<String, Object> params);

    void insertFraudTypeMap(Map<String, Object> params);

    // 필수 체크 항목
    void insertCheckResult(
            @Param("reportId") Long reportId,
            @Param("checkType") CheckType checkType,
            @Param("riskLevel") RiskLevel riskLevel
    );

    // 상세 항목
    void insertDetailResult(
            @Param("fraudTypeId") Long fraudTypeId,
            @Param("detailType") DetailType detailType,
            @Param("riskLevel") RiskLevel riskLevel
    );

    // 리스트 목록
    List<ReportListItem> findReportsByAccountId(@Param("accountId") Long accountId);
    // 리스트 전체 개수
    int countReportsByAccountId(@Param("accountId") Long accountId);
}