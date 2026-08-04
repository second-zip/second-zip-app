package com.secondzip.backend.checklist.service;

import com.secondzip.backend.checklist.dto.request.ChecklistCheckRequestDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistResponseDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChecklistService {
    List<ChecklistResponseDTO> getChecklists(
            Long accountId,
            String category
    );

    void updateCheckStatus(
            Long accountId,
            Long checklistItemId,
            ChecklistCheckRequestDTO request
    );

    void resetChecklist(Long accountId);
}
