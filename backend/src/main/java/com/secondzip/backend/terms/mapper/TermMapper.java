package com.secondzip.backend.terms.mapper;

import com.secondzip.backend.terms.domain.Term;
import com.secondzip.backend.terms.dto.response.TermConsentResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TermMapper {

    List<Term> findLatestTerms();

    Term findById(Long termId);

    List<TermConsentResponseDTO> findConsentsByAccountId(Long accountId);

    TermConsentResponseDTO findConsent(@Param("accountId") Long accountId, @Param("termId") Long termId);

    int upsertConsent(@Param("accountId") Long accountId, @Param("termId") Long termId, @Param("agreed") Boolean agreed);
}