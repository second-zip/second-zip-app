package com.secondzip.backend.checklist.service;

import com.secondzip.backend.checklist.domain.ReportChecklistVO;
import com.secondzip.backend.checklist.dto.request.ChecklistCheckRequestDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistListResponseDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistResponseDTO;
import com.secondzip.backend.checklist.dto.response.ReportChecklistConditionDTO;
import com.secondzip.backend.checklist.mapper.ReportChecklistMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistServiceImpl implements ChecklistService {

    private final ReportChecklistMapper reportChecklistMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistListResponseDTO> getChecklistList(
            Long accountId
    ) {

        return reportChecklistMapper.findChecklistList(
                accountId
        );
    }

    @Override
    @Transactional
    public Long createChecklist(
            Long accountId,
            Long analysisReportId
    ) {

        ReportChecklistConditionDTO condition =
                reportChecklistMapper.findReportCondition(
                        analysisReportId,
                        accountId
                );

        if (condition == null
                || condition.getHousingCategory() == null
                || condition.getHousingCategory().isBlank()) {

            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "리포트 분석이 완료되지 않았거나 주택 유형이 없습니다."
            );
        }

        if (condition.getHousingCategory() == null
                || condition.getHousingCategory().isBlank()) {

            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "리포트의 주택 유형이 확정되지 않았습니다."
            );
        }


        Long existingId =
                reportChecklistMapper
                        .findChecklistIdByReportId(
                                analysisReportId
                        );

        if (existingId != null) {
            return existingId;
        }


        ReportChecklistVO checklist =
                ReportChecklistVO.builder()
                        .analysisReportId(
                                analysisReportId
                        )
                        .accountId(accountId)
                        .build();

        try {

            reportChecklistMapper.insertChecklist(
                    checklist
            );

        } catch (DuplicateKeyException e) {

            // 사용자가 버튼을 동시에 두 번 눌러도
            // DB UNIQUE로 최종 중복 방어
            return reportChecklistMapper
                    .findChecklistIdByReportId(
                            analysisReportId
                    );
        }


        reportChecklistMapper.insertChecklistItems(
                checklist.getReportChecklistId(),
                analysisReportId,
                condition.getHousingCategory(),
                Boolean.TRUE.equals(
                        condition.getTrustProperty()
                )
        );

        return checklist.getReportChecklistId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistResponseDTO> getChecklist(
            Long accountId,
            Long reportChecklistId
    ) {

        return reportChecklistMapper.findChecklistItems(
                accountId,
                reportChecklistId
        );
    }

    @Override
    @Transactional
    public void updateCheckStatus(
            Long accountId,
            Long reportChecklistId,
            Long checklistItemId,
            ChecklistCheckRequestDTO request
    ) {

        int updated =
                reportChecklistMapper.updateChecked(
                        accountId,
                        reportChecklistId,
                        checklistItemId,
                        request.getChecked()
                );

        if (updated != 1) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "체크리스트 항목을 찾을 수 없습니다."
            );
        }
    }


    @Override
    @Transactional
    public void resetChecklist(
            Long accountId,
            Long reportChecklistId
    ) {

        reportChecklistMapper.reset(
                accountId,
                reportChecklistId
        );
    }
}