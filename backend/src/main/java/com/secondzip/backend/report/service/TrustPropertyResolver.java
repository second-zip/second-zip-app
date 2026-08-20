package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.external.RegistryData;
import org.springframework.stereotype.Component;

/**
 * 리포트에 저장할 신탁주택 여부(analysis_reports.is_trust_property)를 판정.
 *
 * 이 값은 위험도 판정용이 아니라 체크리스트 구성용.
 * TRUE면 체크리스트 생성 시 TRUST_PROPERTY 카테고리 항목이 추가.
 *
 * [판정 기준]
 *  등기 갑구에 신탁등기가 있으면 TRUE
*   등기상 소유자가 신탁회사면 TRUE (신탁등기 표시를 파싱하지 못한 경우 대비)
 *  둘 다 아니거나 데이터가 없으면 FALSE
 *
 *
 * 체크리스트는 "확인해보라"는 안내일 뿐 과금이나 차단을 유발하지 않으므로,
 * 판정이 애매하면 항목을 빼기보다 넣는 쪽(누락 방지)을 택.
 */
@Component
public class TrustPropertyResolver {

    private static final String TRUST_COMPANY = "TRUST_COMPANY";

    public boolean resolve(RegistryData registry) {
        if (registry == null) {
            return false;
        }

        if (Boolean.TRUE.equals(registry.getHasTrustRegistration())) {
            return true;
        }

        return TRUST_COMPANY.equals(registry.getOwnerType());
    }
}
