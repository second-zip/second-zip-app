package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.RiskLevel;

import java.util.List;
import java.util.Objects;

public final class RiskAggregation {

    private static final int CHECK_DANGER_THRESHOLD = 3;
    private static final int OVERALL_DANGER_THRESHOLD = 3;

    private RiskAggregation() {
    }

    public static RiskLevel aggregateChecks(List<JudgementDTO> judgements) {
        return aggregate(judgements, CHECK_DANGER_THRESHOLD);
    }

    /**
     * 필수점검 대표값 1개와 사기 유형 대표값 3개를 최종 집계한다.
     * DANGER가 하나라도 있거나 CAUTION이 3개 이상이면 DANGER다.
     */
    public static RiskLevel aggregateOverall(List<RiskLevel> levels) {
        if (levels == null) {
            return RiskLevel.SAFE;
        }

        return RiskLevel.aggregateByCount(
                levels.stream().filter(Objects::nonNull).toList(),
                OVERALL_DANGER_THRESHOLD
        );
    }

    public static RiskLevel aggregateDetails(List<JudgementDTO> judgements) {
        List<JudgementDTO> values = validValues(judgements);

        int applicableCount = (int) values.stream()
                .filter(JudgementDTO::isApplicable)
                .count();

        return aggregate(values, Math.max(1, applicableCount));
    }

    private static RiskLevel aggregate(
            List<JudgementDTO> judgements,
            int dangerThreshold
    ) {
        List<JudgementDTO> values = validValues(judgements);

        List<RiskLevel> verifiedLevels = values.stream()
                .filter(JudgementDTO::isApplicable)
                .filter(judgement -> judgement.dataStatus() == DataStatus.VERIFIED)
                .map(JudgementDTO::riskLevel)
                .toList();

        RiskLevel verifiedResult = RiskLevel.aggregateByCount(
                verifiedLevels,
                dangerThreshold
        );

        boolean hasUnverified = values.stream()
                .anyMatch(judgement ->
                        judgement.dataStatus() == DataStatus.UNVERIFIED
                );

        return verifiedResult == RiskLevel.SAFE && hasUnverified
                ? RiskLevel.CAUTION
                : verifiedResult;
    }

    private static List<JudgementDTO> validValues(
            List<JudgementDTO> judgements
    ) {
        if (judgements == null) {
            return List.of();
        }

        return judgements.stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
