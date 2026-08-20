package com.secondzip.backend.checklist.mapper;

import com.secondzip.backend.checklist.domain.ReportChecklist;
import com.secondzip.backend.checklist.dto.response.*;
import com.secondzip.backend.checklist.enums.Category;
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
            ReportChecklist checklist
    );

    int insertChecklistItems(
            @Param("reportChecklistId")
            Long reportChecklistId,
            @Param("analysisReportId")
            Long analysisReportId,
            @Param("category")
            Category category,
            @Param("trustProperty")
            Boolean trustProperty
    );

    List<ChecklistListResponseDTO> findChecklistList(
            @Param("accountId")
            Long accountId
    );

    ChecklistAddressDTO findChecklistAddress(
            @Param("accountId") Long accountId,
            @Param("reportChecklistId") Long reportChecklistId
    );

    List<ChecklistResponseDTO> findChecklistItems(
            @Param("accountId") Long accountId,
            @Param("reportChecklistId") Long reportChecklistId
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