package com.secondzip.backend.terms.service;

import com.secondzip.backend.terms.dto.request.UpdateTermConsentDTO;
import com.secondzip.backend.terms.dto.response.TermConsentResponseDTO;
import com.secondzip.backend.terms.dto.response.TermResponseDTO;

import java.util.List;

public interface TermService {
    List<TermConsentResponseDTO> getMyConsents(Long accountId);

    TermConsentResponseDTO updateConsent(
            Long accountId,
            Long termId,
            UpdateTermConsentDTO request
    );

    List<TermResponseDTO> getLatestTerms();
}
