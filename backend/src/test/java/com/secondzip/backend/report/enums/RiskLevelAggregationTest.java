package com.secondzip.backend.report.enums;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskLevelAggregationTest {

    @Test
    void selectsTheWorstLevelFromVarargs() {
        assertThat(RiskLevel.worstOf(RiskLevel.SAFE, RiskLevel.DANGER, RiskLevel.CAUTION))
                .isEqualTo(RiskLevel.DANGER);
    }

    @Test
    void treatsAnEmptyEvaluationAsSafe() {
        assertThat(RiskLevel.worstOf(List.of())).isEqualTo(RiskLevel.SAFE);
    }
}
