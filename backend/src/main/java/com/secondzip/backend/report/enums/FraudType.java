package com.secondzip.backend.report.enums;

/** 전세사기 유형 3종. DB fraud_type ENUM과 이름 일치. */
public enum FraudType {
    UNDERWATER_JEONSE,                        // 깡통전세
    FALSE_INFORMATION_RIGHTS_CONCEALMENT,     // 권리은폐
    TRUST_PROPERTY_FRAUD                      // 신탁사기
}