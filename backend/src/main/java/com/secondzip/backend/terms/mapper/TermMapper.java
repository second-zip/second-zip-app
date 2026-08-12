package com.secondzip.backend.terms.mapper;

import com.secondzip.backend.terms.domain.TermVO;
import com.secondzip.backend.terms.dto.response.TermConsentResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TermMapper {

    List<TermVO> findLatestTerms();

    TermVO findById(Long termId);

    List<TermConsentResponseDTO> findConsentsByAccountId(Long accountId);

    TermConsentResponseDTO findConsent(@Param("accountId") Long accountId, @Param("termId") Long termId);

    int upsertConsent(@Param("accountId") Long accountId, @Param("termId") Long termId, @Param("agreed") Boolean agreed);
}