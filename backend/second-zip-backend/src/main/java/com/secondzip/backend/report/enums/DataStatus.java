package com.secondzip.backend.report.enums;

/**
 * 판정에 쓰인 데이터의 상태.
 *
 * <p>{@link RiskLevel}만으로는 <b>"위험해서 주의"</b>와 <b>"몰라서 주의"</b>를 구분할 수 없다.
 * 외부 API가 죽어서 아무것도 못 가져온 리포트가 사용자 화면에는 "위험"으로 보이는 문제가
 * 있어, 위험도와는 별개의 축으로 데이터 상태를 함께 내려준다.
 *
 * <p>RiskLevel은 그대로 3단계를 유지한다. 이 값은 표시 목적이며,
 * {@code NOT_APPLICABLE}만 집계에서 제외된다.
 */
public enum DataStatus {

    /** 데이터를 확보해 실제로 판정했다. */
    VERIFIED,

    /** 데이터를 확보하지 못해 판정할 수 없었다. (외부 API 실패, 파싱 실패 등) */
    UNVERIFIED,

    /** 이 매물에는 애초에 해당하지 않는 항목이다. (예: 집합건물의 토지 소유자 대조) */
    NOT_APPLICABLE
}
