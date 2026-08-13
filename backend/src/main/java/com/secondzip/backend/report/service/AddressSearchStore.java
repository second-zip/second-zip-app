package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.AnalysisTarget;

/**
 * 주소 검색 후보의 식별값을 보관한다.
 *
 * <p>검색 시점에 카카오가 준 법정동코드·본번·부번을 서버가 들고 있다가,
 * 분석 요청 때 {@code addressId}로 꺼내 쓴다. 그래서 같은 주소를 두 번 검색하지 않는다.
 */
public interface AddressSearchStore {

    /** 후보를 보관하고 식별자를 발급한다. */
    String save(AnalysisTarget target);

    /**
     * 보관된 후보를 조회한다.
     *
     * @throws com.secondzip.backend.common.exception.BusinessException 없거나 만료된 경우
     */
    AnalysisTarget find(String addressId);
}
