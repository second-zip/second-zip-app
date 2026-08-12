package com.secondzip.backend.report.service;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.SpecialTermResult;
import com.secondzip.backend.report.dto.response.SpecialTermView;
import com.secondzip.backend.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialTermPersistenceService {

    private final ReportMapper reportMapper;

    @Transactional
    public List<SpecialTermView> replace(Long reportId, List<SpecialTermResult> terms) {
        Long lockedReportId = reportMapper.lockReportById(reportId);

        if (lockedReportId == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND, "리포트를 찾을 수 없습니다."
            );
        }

        reportMapper.deleteSpecialTermsByReportId(reportId);

        List<SpecialTermView> savedTerms = new ArrayList<>();

        for (int index = 0; index < terms.size(); index++) {
            SpecialTermResult term = terms.get(index);
            reportMapper.insertSpecialTerm(reportId, term.getTitle(), term.getContent());
            savedTerms.add(new SpecialTermView(index + 1, term.getTitle(), term.getContent()));
        }

        return savedTerms;
    }
}
