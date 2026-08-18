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
 * <h3>왜 RiskLevel 이 아니라 DataStatus 로 판단하는가</h3>
 * 체크리스트는 "이 매물이 안전한가"가 아니라 "사용자가 이 항목을 확인했는가"를 묻는 도구다.
 * 근저당이 잔뜩 잡혀 있어 DANGER 로 판정됐더라도, 그건 등기부를 확인했다는 뜻이므로
 * '등기부등본 확인' 항목은 완료된 것이 맞다. 반대로 외부 API 가 죽어서 아무것도 못 가져온
 * UNVERIFIED 는 위험도와 무관하게 확인되지 않은 것이다.
 * 그래서 자동 체크의 기준은 {@link DataStatus#VERIFIED} 하나뿐이다.
 *
 * <h3>자동 체크하지 않는 항목</h3>
 * <ul>
 *   <li>'잔금 지급 직전 등기부 재확인' — 분석 시점에 아직 일어나지 않은 미래의 행위다.</li>
 *   <li>'국세·지방세 체납' — 대응하는 외부 데이터 소스가 없다.</li>
 *   <li>'대리계약 여부', '신탁회사 동의', '전입세대확인서' 등 — 현장/서류 확인 항목이다.</li>
 *   <li>다가구(MULTI_FAMILY) 전 항목 — 확정일자 부여현황 열람이 필요해 현재 데이터로 판단할 수 없다.</li>
 * </ul>
 * 자동 체크 대상을 늘리는 것보다, 확인하지 않은 것을 확인했다고 표시하지 않는 쪽이 중요하다.
 *
 * <h3>COMMON 과 유형별 항목의 관계</h3>
 * 체크리스트는 "COMMON + 주택유형(+ TRUST_PROPERTY)"으로 조립된다.
 * 유형별 항목이 COMMON 과 같은 판정을 근거로 삼으면 사용자 화면에
 * 사실상 같은 항목이 두 줄로 뜬다. (예: '전세가율 확인' + '전세가율')
 * 그래서 V8 에서 중복 항목을 제거했고, 여기 규칙도 그에 맞춰 정리했다.
 * <b>새 규칙을 추가할 때 COMMON 규칙과 근거 판정이 겹치는지 반드시 확인할 것.</b>
 *
 * <p>의존성이 없는 순수 규칙이라 스프링 빈으로 만들지 않고 정적 유틸로 둔다.
 * ({@code BuildingRegisterDocumentSelector} 와 같은 패턴)
 */
public final class ChecklistAutoCheckResolver {

    private ChecklistAutoCheckResolver() {
    }

    /**
     * 체크리스트 항목 하나와, 그 항목을 "확인 완료"로 만들기 위해 필요한 판정 항목들.
     * 근거 항목이 여러 개면 <b>전부</b> VERIFIED 여야 한다.
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

        private boolean isVerifiedBy(
                Map<CheckType, DataStatus> checkStatuses,
                Map<DetailType, DataStatus> detailStatuses
        ) {
            for (CheckType checkType : requiredChecks) {
                if (checkStatuses.get(checkType) != DataStatus.VERIFIED) {
                    return false;
                }
            }
            for (DetailType detailType : requiredDetails) {
                if (detailStatuses.get(detailType) != DataStatus.VERIFIED) {
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
            byChecks(Category.COMMON, "등기부등본 확인",
                    CheckType.MORTGAGE_EXISTENCE, CheckType.RIGHTS_INFRINGEMENT),
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
            // V8에서 유형별 고유 항목이 전부 제거됨(COMMON과 중복).
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
     *
     * <p>리포트의 주택 유형과 무관하게 전 카테고리 규칙을 평가한다.
     * 실제로 어떤 항목이 체크리스트에 붙을지는 체크리스트 생성 시점의
     * {@code insertChecklistItems} 가 category 로 걸러 주므로,
     * 여기서 유형을 한 번 더 판단하면 규칙이 두 곳으로 흩어진다.
     *
     * @return 확인 완료로 표시할 항목들. 없으면 빈 리스트.
     */
    public static List<VerifiedChecklistItem> resolve(RiskEvaluationResult evaluation) {

        if (evaluation == null) {
            return List.of();
        }

        Map<CheckType, DataStatus> checkStatuses = new EnumMap<>(CheckType.class);
        for (CheckResult checkResult : nullSafe(evaluation.getCheckResults())) {
            if (checkResult != null && checkResult.getCheckType() != null) {
                checkStatuses.put(checkResult.getCheckType(), checkResult.getDataStatus());
            }
        }

        Map<DetailType, DataStatus> detailStatuses = new EnumMap<>(DetailType.class);
        for (FraudTypeResult fraudType : nullSafe(evaluation.getFraudTypeResults())) {
            if (fraudType == null) {
                continue;
            }
            for (DetailResult detail : nullSafe(fraudType.getDetails())) {
                if (detail != null && detail.getDetailType() != null) {
                    detailStatuses.put(detail.getDetailType(), detail.getDataStatus());
                }
            }
        }

        List<VerifiedChecklistItem> verified = new ArrayList<>();
        for (Rule rule : RULES) {
            if (rule.isVerifiedBy(checkStatuses, detailStatuses)) {
                verified.add(new VerifiedChecklistItem(rule.category, rule.contents));
            }
        }
        return List.copyOf(verified);
    }

    private static <T> Collection<T> nullSafe(Collection<T> source) {
        return source == null ? List.of() : source;
    }
}
