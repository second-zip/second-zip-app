package com.secondzip.backend.report.dto;

/**
 * 외부 API 조회를 위한 '표준화된 주소 식별값'을 담는 불변 객체
 */
public record AnalysisTarget(
        String originalAddress,      // 사용자가 입력한 원본 주소 (예: 서울 강남구 테헤란로 11)
        String roadAddress,          // 정제된 표준 도로명 주소
        String legalDongCode,        // 법정동 코드 10자리 (API 문서상 LAWD_CD는 5자리만 필요)
        String sigunguCode,          // 시군구 코드 5자리
        String bjdongCode,           // 읍면동(법정동) 코드 5자리
        String mainNo,
        String subNo,
        String roadBuildingMainNo,
        String roadBuildingSubNo,
        String buildingManagementNo,
        String legalDongName,
        String lotAddress
) {
    public AnalysisTarget(
            String originalAddress,
            String roadAddress,
            String legalDongCode,
            String sigunguCode,
            String bjdongCode,
            String mainNo,
            String subNo,
            String roadBuildingMainNo,
            String roadBuildingSubNo,
            String buildingManagementNo
    ) {
        this(
                originalAddress,
                roadAddress,
                legalDongCode,
                sigunguCode,
                bjdongCode,
                mainNo,
                subNo,
                roadBuildingMainNo,
                roadBuildingSubNo,
                buildingManagementNo,
                null,
                null
        );
    }
}
