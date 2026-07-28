package com.secondzip.backend.report.enums;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** 위험 레벨 설정 **/
@RequiredArgsConstructor
public enum RiskLevel {
    SAFE(0), CAUTION(1), DANGER(2);

    private final int priority;

    /** 최대 위험 판정 (하나라도 Danger -> Danger) 로직 **/
    public static RiskLevel worstOf(List<RiskLevel> levels) {
        return levels.stream()
                .max(Comparator.comparingInt(l -> l.priority))
                .orElse(SAFE);
    }

    public static RiskLevel worstOf(RiskLevel... levels) {
        return worstOf(Arrays.asList(levels));
    }

    /**
     * 개수 기반 집계
     * DANGER 1개 이상 → DANGER
     * DANGER 0 & CAUTION >= dangerThreshold(3개 또는 전체) → DANGER
     * DANGER 0 & CAUTION 1개 이상 → CAUTION
     * 전부 SAFE → SAFE
     */
    public static RiskLevel aggregateByCount(List<RiskLevel> levels, int dangerThreshold) {
        long dangerCount = levels.stream().filter(l -> l == DANGER).count();
        if (dangerCount >= 1) return DANGER;

        long cautionCount = levels.stream().filter(l -> l == CAUTION).count();
        if (cautionCount >= dangerThreshold) return DANGER;
        if (cautionCount >= 1) return CAUTION;

        return SAFE;
    }
}
