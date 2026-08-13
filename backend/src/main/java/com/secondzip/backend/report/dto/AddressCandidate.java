package com.secondzip.backend.report.dto;

/**
 * 주소 검색 결과 후보 1건.
 *
 * <p>{@link AnalysisTarget}은 외부 API 조회에 필요한 식별값만 담는다.
 * 우편번호는 화면 표시 전용이라 거기에 넣지 않고 여기서 함께 들고 다닌다.
 */
public record AddressCandidate(
        AnalysisTarget target,
        String zoneNo
) {
}
