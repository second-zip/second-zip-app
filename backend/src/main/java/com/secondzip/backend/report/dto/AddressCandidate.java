package com.secondzip.backend.report.dto;

// 주소 검색 결과 후보 1건.
// AnalysisTarget은 외부 API 조회용 식별값만 담는다.
// 우편번호와 건물명은 화면 표시 전용이라 여기서 함께 들고 다닌다.
public record AddressCandidate(
        AnalysisTarget target,
        String zoneNo,
        String placeName
) {

    public AddressCandidate(AnalysisTarget target, String zoneNo) {
        this(target, zoneNo, null);
    }

    // 장소 검색으로 찾은 후보에 건물명을 붙인다.
    public AddressCandidate withPlaceName(String placeName) {
        return new AddressCandidate(target, zoneNo, placeName);
    }
}
