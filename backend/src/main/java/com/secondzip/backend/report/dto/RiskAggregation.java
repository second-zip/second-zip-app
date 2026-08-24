package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.RiskLevel;

import java.util.List;
import java.util.Objects;

public final class RiskAggregation {

    private static final int CHECK_DANGER_THRESHOLD = 3;

    private RiskAggregation() {
    }

    public static RiskLevel aggregateChecks(List<JudgementDTO> judgements) {
        return aggregate(judgements, CHECK_DANGER_THRESHOLD);
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