package com.secondzip.backend.checklist.service;

import com.secondzip.backend.checklist.dto.request.ChecklistCheckRequestDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistListResponseDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistResponseDTO;

import java.util.List;

public interface ChecklistService {

    List<ChecklistListResponseDTO> getChecklistList(
            Long accountId
    );

    Long createChecklist(
            Long accountId,
            Long analysisReportId
    );

    List<ChecklistResponseDTO> getChecklist(
            Long accountId,
            Long reportChecklistId
    );

    void updateCheckStatus(
            Long accountId,
            Long reportChecklistId,
            Long checklistItemId,
            ChecklistCheckRequestDTO request
    );

    void resetChecklist(
            Long accountId,
            Long reportChecklistId
    );
}