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
 *  신탁등기가 없고 소유자 유형도 비신탁으로 확인된 경우에만 FALSE
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
            return true;
        }

        if (Boolean.TRUE.equals(registry.getHasTrustRegistration())) {
            return true;
        }

        if (TRUST_COMPANY.equals(registry.getOwnerType())) {
            return true;
        }

        // 신탁등기와 소유자 유형 중 하나라도 판독하지 못했다면 확인용
        // 체크리스트를 남긴다. 두 값이 모두 명시적으로 비신탁일 때만 제외한다.
        return !Boolean.FALSE.equals(registry.getHasTrustRegistration())
                || registry.getOwnerType() == null;
    }
}
