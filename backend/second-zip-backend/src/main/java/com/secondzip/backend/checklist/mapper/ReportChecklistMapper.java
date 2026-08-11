package com.secondzip.backend.checklist.mapper;

import com.secondzip.backend.checklist.domain.ReportChecklistVO;
import com.secondzip.backend.checklist.dto.response.ChecklistListResponseDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistResponseDTO;
import com.secondzip.backend.checklist.dto.response.ReportChecklistConditionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportChecklistMapper {

    Long findChecklistIdByReportId(
            @Param("analysisReportId")
            Long analysisReportId
    );

    String findHousingCategoryByReportIdAndAccountId(
            @Param("analysisReportId")
            Long analysisReportId,
            @Param("accountId")
            Long accountId
    );

    ReportChecklistConditionDTO findReportCondition(
            @Param("analysisReportId")
            Long analysisReportId,
            @Param("accountId")
            Long accountId
    );

    int insertChecklist(
            ReportChecklistVO checklist
    );

    int insertChecklistItems(
            @Param("reportChecklistId")
            Long reportChecklistId,
            @Param("analysisReportId")
            Long analysisReportId,
            @Param("category")
            String category,
            @Param("trustProperty")
            Boolean trustProperty
    );

    List<ChecklistListResponseDTO> findChecklistList(
            @Param("accountId")
            Long accountId
    );

    List<ChecklistResponseDTO> findChecklistItems(
            @Param("accountId")
            Long accountId,
            @Param("reportChecklistId")
            Long reportChecklistId
    );

    int updateChecked(
            @Param("accountId")
            Long accountId,
            @Param("reportChecklistId")
            Long reportChecklistId,
            @Param("checklistItemId")
            Long checklistItemId,
            @Param("checked")
            Boolean checked
    );

    int markChecked(
            @Param("accountId")
            Long accountId,
            @Param("reportChecklistId")
            Long reportChecklistId,
            @Param("checklistItemId")
            Long checklistItemId
    );

    int reset(
            @Param("accountId")
            Long accountId,
            @Param("reportChecklistId")
            Long reportChecklistId
    );

    int existsOwnedChecklist(
            @Param("accountId")
            Long accountId,
            @Param("reportChecklistId")
            Long reportChecklistId
    );
}