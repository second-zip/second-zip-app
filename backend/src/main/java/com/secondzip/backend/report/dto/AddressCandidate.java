package com.secondzip.backend.report.dto;

/**
 * 주소 검색 결과 한 건.
 *
 * AnalysisTarget에 우편번호를 넣지 않고 따로 묶는 이유:
 * AnalysisTarget은 AnalysisWorkflowState에 담겨 Redis에 JSON으로 저장된다.
 * 필드를 늘리면 직렬화 포맷이 바뀌고, 분석에 쓰이지도 않는 값이 워크플로 상태에 남는다.
 * 우편번호는 검색 화면 표시용이므로 여기서만 들고 있는다.
 */
public record AddressCandidate(
        AnalysisTarget target,
        String zoneNo
) {
}