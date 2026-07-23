package com.secondzip.backend.report.mapper;

import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

@Mapper
public interface ReportMapper {
    void insertReportMap(Map<String, Object> params);
    void insertFraudTypeMap(Map<String, Object> params);

    void insertCheckResult(
            @Param("reportId") Long reportId,
            @Param("checkType") CheckType checkType,
            @Param("riskLevel") RiskLevel riskLevel
    );

    void insertDetailResult(
            @Param("fraudTypeId") Long fraudTypeId,
            @Param("detailType") DetailType detailType,
            @Param("riskLevel") RiskLevel riskLevel
    );
}