package com.secondzip.backend.report.mapper;

import com.secondzip.backend.report.domain.AnalysisReport;
import com.secondzip.backend.report.domain.ReportCheckResult;
import com.secondzip.backend.report.domain.ReportFraudType;
import com.secondzip.backend.report.domain.ReportShareInfo;
import com.secondzip.backend.report.domain.ReportSpecialTerm;
import com.secondzip.backend.report.dto.DetailResultDTO;
import com.secondzip.backend.report.dto.VerifiedChecklistItemDTO;
import com.secondzip.backend.report.dto.response.ReportListItem;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;

import java.util.List;

@Mapper
public interface ReportMapper {

    // 리포트 생성. 저장 후 report.analysisReportId 에 생성된 PK 가 채워진다.
    void insertReport(AnalysisReport report);

    // 필수 항목 5
    void insertCheckResult(ReportCheckResult checkResult);

    // 사기 유형 3. 저장 후 fraudType.reportFraudTypeId 에 생성된 PK 가 채워진다.
    void insertFraudType(ReportFraudType fraudType);

    // 상세 항목
    void insertDetailResult(
            @Param("fraudTypeId") Long fraudTypeId,
            @Param("detailType") DetailType detailType,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("dataStatus") DataStatus dataStatus
    );

    /**
     * 분석으로 확인 완료된 체크리스트 항목 저장.
     * 이후 체크리스트를 생성할 때 insertChecklistItems 가 이 행을 보고
     * 해당 항목을 is_checked = TRUE 로 만든다.
     * items 가 비어 있으면 호출하지 않는다. (IN () 은 문법 오류)
     */
    void insertChecklistVerifications(
            @Param("analysisReportId") Long analysisReportId,
            @Param("items") List<VerifiedChecklistItemDTO> items
    );

    // 리포트 목록 조회
    List<ReportListItem> findReportsByAccountId(@Param("accountId") Long accountId);
    int countReportsByAccountId(@Param("accountId") Long accountId);

    // 리포트 상세 조회
    Long findReportIdByRequestId(
            @Param("accountId") Long accountId,
            @Param("requestId") String requestId
    );
    Long findAccountIdByReportId(@Param("reportId") Long reportId);
    AnalysisReport findReportById(@Param("reportId") Long reportId);
    List<ReportCheckResult> findCheckResultsByReportId(@Param("reportId") Long reportId);
    List<ReportFraudType> findFraudTypesByReportId(@Param("reportId") Long reportId);
    List<DetailResultDTO> findDetailResultsByFraudTypeId(@Param("fraudTypeId") Long fraudTypeId);


    // 특약 저장 처리 중 리포트 잠금
    Long lockReportById(@Param("reportId") Long reportId);

    // AI 특약 조회
    List<ReportSpecialTerm> findSpecialTermsByReportId(@Param("reportId") Long reportId);

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
    ReportShareInfo findShareInfoByReportId(@Param("reportId") Long reportId);
    Long findReportIdByShareToken(@Param("shareToken") String shareToken);


    // 리포트 삭제
    void deleteReport(@Param("reportId") Long reportId);
}
