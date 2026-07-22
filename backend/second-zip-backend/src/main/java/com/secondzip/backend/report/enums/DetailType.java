package com.secondzip.backend.report.enums;

/**
 * 유형별 세부항목 9개. DB detail_type ENUM과 이름 일치.
 * title은 DB에 저장 안 하고 여기(코드)에서 관리.
 */
public enum DetailType {
    HIGH_JEONSE_RATIO(FraudType.UNDERWATER_JEONSE, "높은 전세가율"),
    PRIORITY_DEBT_BURDEN(FraudType.UNDERWATER_JEONSE, "선순위채권 부담"),
    HUG_GUARANTEE_PRECHECK(FraudType.UNDERWATER_JEONSE, "HUG 보증보험 사전점검"),

    LAND_BUILDING_OWNERSHIP_MISMATCH(FraudType.FALSE_INFORMATION_RIGHTS_CONCEALMENT, "건물·토지 소유관계 불일치"),
    FALSE_BUILDING_USE_INFORMATION(FraudType.FALSE_INFORMATION_RIGHTS_CONCEALMENT, "건축물 용도 허위 안내"),
    RIGHTS_INFRINGEMENT_CONCEALMENT(FraudType.FALSE_INFORMATION_RIGHTS_CONCEALMENT, "등기상 권리침해 은폐"),

    TRUST_REGISTRATION_EXISTENCE(FraudType.TRUST_PROPERTY_FRAUD, "신탁등기 존재 여부"),
    REGISTERED_OWNER_VERIFICATION(FraudType.TRUST_PROPERTY_FRAUD, "등기상 소유자 확인"),
    POST_TRUST_RIGHTS_INFRINGEMENT(FraudType.TRUST_PROPERTY_FRAUD, "신탁등기 이후 추가 권리침해 여부");

    private final FraudType parentType;
    private final String title;

    DetailType(FraudType parentType, String title) {
        this.parentType = parentType;
        this.title = title;
    }

    public FraudType getParentType() { return parentType; }
    public String getTitle() { return title; }
}