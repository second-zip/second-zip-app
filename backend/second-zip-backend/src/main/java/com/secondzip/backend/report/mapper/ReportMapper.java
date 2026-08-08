package com.secondzip.backend.report.mapper;

import com.secondzip.backend.report.dto.DetailResult;
import com.secondzip.backend.report.dto.response.ReportListItem;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    // 필수 항목 5
    void insertCheckResult(Map<String, Object> params);

    // 사기 유형 3
    void insertFraudTypeMap(Map<String, Object> params);

    // 상세 항목
    void insertDetailResult(
            @Param("fraudTypeId") Long fraudTypeId,
            @Param("detailType") DetailType detailType,
            @Param("riskLevel") RiskLevel riskLevel
    );

    // 리포트 생성
    void insertReportMap(Map<String, Object> params);

    // 리포트 목록 조회
    List<ReportListItem> findReportsByAccountId(@Param("accountId") Long accountId);
    int countReportsByAccountId(@Param("accountId") Long accountId);

    // 리포트 상세 조회
    Long findReportIdByRequestId(
            @Param("accountId") Long accountId,
            @Param("requestId") String requestId
    );
    Long findAccountIdByReportId(@Param("reportId") Long reportId);
    Map<String, Object> findReportById(@Param("reportId") Long reportId);
    List<Map<String, Object>> findCheckResultsByReportId(@Param("reportId") Long reportId);
    List<Map<String, Object>> findFraudTypesByReportId(@Param("reportId") Long reportId);
    List<DetailResult> findDetailResultsByFraudTypeId(@Param("fraudTypeId") Long fraudTypeId);


    // 특약 저장 처리 중 리포트 잠금
    Long lockReportById(@Param("reportId") Long reportId);

    // AI 특약 조회
    List<Map<String, Object>> findSpecialTermsByReportId(@Param("reportId") Long reportId);

    // AI 특약 저장
    void insertSpecialTerm(
            @Param("reportId") Long reportId,
            @Param("title") String title,
            @Param("content") String content
    );

    // AI 특약 전체 삭제
    void deleteSpecialTermsByReportId(@Param("reportId") Long reportId);


    // 즐겨찾기
    void updateFavorite(
            @Param("reportId") Long reportId,
            @Param("favorite") boolean favorite,
            @Param("favoritedAt") LocalDateTime favoritedAt
    );

    // 공유
    void updateShareToken(
            @Param("reportId") Long reportId,
            @Param("shareToken") String shareToken,
            @Param("shareExpiresAt") LocalDateTime shareExpiresAt
    );
    Map<String, Object> findShareInfoByReportId(@Param("reportId") Long reportId);
    Long findReportIdByShareToken(@Param("shareToken") String shareToken);


    // 리포트 삭제
    void deleteReport(@Param("reportId") Long reportId);
}

