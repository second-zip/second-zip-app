package com.secondzip.backend.report.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** 위험 레벨 설정 **/
public enum RiskLevel {
    SAFE(0), CAUTION(1), DANGER(2);

    private final int priority;
    RiskLevel(int priority) { this.priority = priority; }

    /** 최대 위험 판정 (하나라도 Danger -> Danger) 로직 **/
    public static RiskLevel worstOf(List<RiskLevel> levels) {
        return levels.stream()
                .max(Comparator.comparingInt(l -> l.priority))
                .orElse(SAFE);
    }

    public static RiskLevel worstOf(RiskLevel... levels) {
        return worstOf(Arrays.asList(levels));
    }
}