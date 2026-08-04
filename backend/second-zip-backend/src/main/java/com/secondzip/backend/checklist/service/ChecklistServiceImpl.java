package com.secondzip.backend.checklist.service;

import com.secondzip.backend.checklist.dto.request.ChecklistCheckRequestDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistResponseDTO;
import com.secondzip.backend.checklist.mapper.AccountChecklistItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistServiceImpl implements ChecklistService {

    private final AccountChecklistItemMapper accountChecklistItemMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistResponseDTO> getChecklists(
            Long accountId,
            String category
    ) {
        return accountChecklistItemMapper
                .findByAccountIdAndCategory(
                        accountId,
                        category
                );
    }

    @Override
    @Transactional
    public void updateCheckStatus(
            Long accountId,
            Long checklistItemId,
            ChecklistCheckRequestDTO request
    ) {

        if (Boolean.TRUE.equals(request.getChecked())) {

            accountChecklistItemMapper.insertChecked(
                    accountId,
                    checklistItemId
            );

            return;
        }

        accountChecklistItemMapper.delete(
                accountId,
                checklistItemId
        );
    }

    @Override
    @Transactional
    public void resetChecklist(Long accountId) {
        accountChecklistItemMapper.deleteAllByAccountId(accountId);
    }
}