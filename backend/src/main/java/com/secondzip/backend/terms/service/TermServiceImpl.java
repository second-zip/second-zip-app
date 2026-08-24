package com.secondzip.backend.terms.service;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.terms.domain.Term;
import com.secondzip.backend.terms.dto.request.UpdateTermConsentDTO;
import com.secondzip.backend.terms.dto.response.TermConsentResponseDTO;
import com.secondzip.backend.terms.dto.response.TermResponseDTO;
import com.secondzip.backend.terms.mapper.TermMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermServiceImpl implements TermService {

    private final TermMapper termMapper;

    @Override
    public List<TermConsentResponseDTO> getMyConsents(Long accountId) {
        return termMapper.findConsentsByAccountId(accountId);
    }

    @Override
    @Transactional
    public TermConsentResponseDTO updateConsent(
            Long accountId,
            Long termId,
            UpdateTermConsentDTO request
    ) {
        Term term = termMapper.findById(termId);

        if (term == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "약관을 찾을 수 없습니다.");
        }

        if (Boolean.TRUE.equals(term.getRequired()) && Boolean.FALSE.equals(request.getAgreed())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "필수 약관은 동의를 철회할 수 없습니다.");
        }

        int updatedCount = termMapper.upsertConsent(accountId, termId, request.getAgreed());

        if (updatedCount < 1) {
            throw new IllegalStateException(
                    "약관 동의 상태 변경에 실패했습니다."
            );
        }

        return termMapper.findConsent(accountId, termId);
    }

    @Override
    public List<TermResponseDTO> getLatestTerms() {
        return termMapper.findLatestTerms()
                .stream()
                .map(TermResponseDTO::from)
                .collect(Collectors.toList());
    }
}