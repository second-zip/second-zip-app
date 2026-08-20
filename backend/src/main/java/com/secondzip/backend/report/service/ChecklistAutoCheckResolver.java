package com.secondzip.backend.report.service;

import com.secondzip.backend.checklist.enums.Category;
import com.secondzip.backend.report.dto.CheckResult;
import com.secondzip.backend.report.dto.DetailResult;
import com.secondzip.backend.report.dto.FraudTypeResult;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.VerifiedChecklistItem;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 분석 판정 결과를 체크리스트 자동 체크 항목으로 변환한다.
 *
 * 자동 체크 기준: VERIFIED 이면서 SAFE
 */
public final class ChecklistAutoCheckResolver {

    private ChecklistAutoCheckResolver() {
    }

    /**
     * 판정 하나의 결과(위험도 + 데이터 상태).
     */
    private record Verdict(RiskLevel riskLevel, DataStatus dataStatus) {

        /** 해당 판정 자체가 결과에 없는 경우. */
        private static final Verdict ABSENT = new Verdict(null, null);

        /** 확인(VERIFIED) 안전한(SAFE) 경우에만 참. */
        private boolean isCleared() {
            return dataStatus == DataStatus.VERIFIED && riskLevel == RiskLevel.SAFE;
        }
    }

    /**
     * 체크리스트 항목 하나와, 그 항목을 "확인 완료"로 만들기 위해 필요한 판정 항목들.
     * 근거 항목이 여러 개면 전부 VERIFIED + SAFE 여야 함.
     */
    private static final class Rule {

        private final Category category;
        private final String contents;
        private final Set<CheckType> requiredChecks;
        private final Set<DetailType> requiredDetails;

        private Rule(
                Category category,
                String contents,
                Set<CheckType> requiredChecks,
                Set<DetailType> requiredDetails
        ) {
            if (requiredChecks.isEmpty() && requiredDetails.isEmpty()) {
                throw new IllegalArgumentException(
                        "근거 판정 항목이 없는 규칙은 항상 참이 되어 위험하다: " + contents
                );
            }
            this.category = category;
            this.contents = contents;
            this.requiredChecks = requiredChecks;
            this.requiredDetails = requiredDetails;
        }

        private boolean isClearedBy(
                Map<CheckType, Verdict> checkVerdicts,
                Map<DetailType, Verdict> detailVerdicts
        ) {
            for (CheckType checkType : requiredChecks) {
                if (!checkVerdicts.getOrDefault(checkType, Verdict.ABSENT).isCleared()) {
                    return false;
                }
            }
            for (DetailType detailType : requiredDetails) {
                if (!detailVerdicts.getOrDefault(detailType, Verdict.ABSENT).isCleared()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static Rule byChecks(Category category, String contents, CheckType... checkTypes) {
        return new Rule(
                category,
                contents,
                EnumSet.copyOf(List.of(checkTypes)),
                EnumSet.noneOf(DetailType.class)
        );
    }

    private static Rule byDetails(Category category, String contents, DetailType... detailTypes) {
        return new Rule(
                category,
                contents,
                EnumSet.noneOf(CheckType.class),
                EnumSet.copyOf(List.of(detailTypes))
        );
    }

    /**
     * contents 문자열은 checklist_items 시드 데이터(V1)와 정확히 일치해야 한다.
     * 하나라도 어긋나면 조용히 매칭 0건이 되므로, 시드를 수정하면 여기도 함께 고친다.
     */
    private static final List<Rule> RULES = List.of(

            // ---- 공통 ----
            // 근저당이 하나라도 잡혀 있으면 MORTGAGE_EXISTENCE 가 CAUTION 이상이라
            // 이 항목은 체크되지 않는다. (근저당 없는 매물만 자동 체크)
            byChecks(Category.COMMON, "등기부등본 확인",
                    CheckType.MORTGAGE_EXISTENCE, CheckType.RIGHTS_INFRINGEMENT),
            // 업무용 오피스텔은 BUILDING_USE 가 CAUTION 이라 체크되지 않는다.
            byChecks(Category.COMMON, "건축물대장 확인",
                    CheckType.ILLEGAL_BUILDING, CheckType.BUILDING_USE),
            byChecks(Category.COMMON, "HUG/HF/SGI 보증보험 가능 여부 확인",
                    CheckType.HUG_GUARANTEE_ELIGIBILITY),
            byDetails(Category.COMMON, "전세가율 확인",
                    DetailType.HIGH_JEONSE_RATIO),

            // ---- 단독주택 ----
            byChecks(Category.SINGLE_FAMILY, "위반건축물 여부",
                    CheckType.ILLEGAL_BUILDING),
            byDetails(Category.SINGLE_FAMILY, "건물·토지 소유자 동일 여부",
                    DetailType.LAND_BUILDING_OWNERSHIP_MISMATCH),

            // ---- 아파트 ----
            // 아파트는 COMMON 규칙만 적용된다.

            // ---- 다세대·연립 ----
            byChecks(Category.MULTI_HOUSEHOLD, "공동근저당",
                    CheckType.MORTGAGE_EXISTENCE),
            byChecks(Category.MULTI_HOUSEHOLD, "위반건축물",
                    CheckType.ILLEGAL_BUILDING),

            // ---- 오피스텔 ----
            byChecks(Category.OFFICETEL, "주거용/업무용 여부",
                    CheckType.BUILDING_USE),
            byDetails(Category.OFFICETEL, "신탁등기 여부",
                    DetailType.TRUST_REGISTRATION_EXISTENCE)
    );

    /**
     * 판정 결과에서 자동 체크할 체크리스트 항목을 뽑아낸다.
     * 확인 완료(VERIFIED + SAFE)로 표시할 항목들. 없으면 빈 리스트.
     */
    public static List<VerifiedChecklistItem> resolve(RiskEvaluationResult evaluation) {

        if (evaluation == null) {
            return List.of();
        }

        Map<CheckType, Verdict> checkVerdicts = new EnumMap<>(CheckType.class);
        for (CheckResult checkResult : nullSafe(evaluation.getCheckResults())) {
            if (checkResult != null && checkResult.getCheckType() != null) {
                checkVerdicts.put(
                        checkResult.getCheckType(),
                        new Verdict(checkResult.getRiskLevel(), checkResult.getDataStatus())
                );
            }
        }

        Map<DetailType, Verdict> detailVerdicts = new EnumMap<>(DetailType.class);
        for (FraudTypeResult fraudType : nullSafe(evaluation.getFraudTypeResults())) {
            if (fraudType == null) {
                continue;
            }
            for (DetailResult detail : nullSafe(fraudType.getDetails())) {
                if (detail != null && detail.getDetailType() != null) {
                    detailVerdicts.put(
                            detail.getDetailType(),
                            new Verdict(detail.getRiskLevel(), detail.getDataStatus())
                    );
                }
            }
        }

        List<VerifiedChecklistItem> verified = new ArrayList<>();
        for (Rule rule : RULES) {
            if (rule.isClearedBy(checkVerdicts, detailVerdicts)) {
                verified.add(new VerifiedChecklistItem(rule.category, rule.contents));
            }
        }
        return List.copyOf(verified);
    }

    private static <T> Collection<T> nullSafe(Collection<T> source) {
        return source == null ? List.of() : source;
    }
}
