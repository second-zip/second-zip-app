package com.secondzip.backend.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * analysis_reports 의 공유 관련 컬럼만 뽑은 조회 결과.
 *
 * 공유 링크 재발급 판단에 두 컬럼만 필요해서, AnalysisReport 를 절반만 채워
 * 돌려주는 대신 전용 타입을 둠.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportShareInfo {

    private String shareToken;
    private LocalDateTime shareExpiresAt;
}
