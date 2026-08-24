package com.secondzip.backend.report.service;

import com.secondzip.backend.checklist.enums.Category;
import com.secondzip.backend.report.dto.*;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.FraudType;
import com.secondzip.backend.report.enums.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChecklistAutoCheckResolverTest {

    // ---------- 헬퍼 ----------

    private static CheckResultDTO check(CheckType type, RiskLevel risk, DataStatus status) {
        return new CheckResultDTO(type, risk, status, Map.of());
    }

    private static RiskEvaluationResultDTO evaluation(
            List<CheckResultDTO> checks,
            List<DetailResultDTO> details
    ) {
        List<FraudTypeResultDTO> fraudTypes = new ArrayList<>();
        for (FraudType fraudType : FraudType.values()) {
            List<DetailResultDTO> owned = details.stream()
                    .filter(d -> d.getDetailType().getParentType() == fraudType)
                    .collect(Collectors.toList());
            if (!owned.isEmpty()) {
                fraudTypes.add(new FraudTypeResultDTO(fraudType, RiskLevel.SAFE, owned));
            }
        }
        return new RiskEvaluationResultDTO(RiskLevel.SAFE, checks, fraudTypes);
    }

    /** 필수점검 5개 전부 + 세부 9개 전부를 SAFE + 주어진 데이터 상태로 채운 판정 결과 */
    private static RiskEvaluationResultDTO allWith(DataStatus status) {
        List<CheckResultDTO> checks = Arrays.stream(CheckType.values())
                .map(t -> check(t, RiskLevel.SAFE, status))
                .collect(Collectors.toList());
        List<DetailResultDTO> details = Arrays.stream(DetailType.values())
                .map(t -> new DetailResultDTO(t, RiskLevel.SAFE, status))
                .collect(Collectors.toList());
        return evaluation(checks, details);
    }

    private static Set<String> contentsOf(List<VerifiedChecklistItemDTO> items) {
        return items.stream()
                .map(VerifiedChecklistItemDTO::getContents)
                .collect(Collectors.toSet());
    }

    // ---------- 기본 동작 ----------

    @Test
    @DisplayName("전부 VERIFIED + SAFE면 규칙에 정의된 항목이 모두 확인 완료로 나온다")
    void resolvesAllRulesWhenEveryJudgementIsVerifiedAndSafe() {
        List<VerifiedChecklistItemDTO> result =
                ChecklistAutoCheckResolver.resolve(allWith(DataStatus.VERIFIED));

        assertEquals(9, result.size());
        assertTrue(contentsOf(result).containsAll(Set.of(
                "등기부등본 확인",
                "건축물대장 확인",
                "전세가율 확인"
        )));
        assertFalse(contentsOf(result).contains("HUG/HF/SGI 보증보험 가능 여부 확인"));
    }

    @Test
    @DisplayName("외부 API 실패로 UNVERIFIED면 어떤 항목도 자동 체크하지 않는다")
    void resolvesNothingWhenEveryJudgementIsUnverified() {
        assertTrue(ChecklistAutoCheckResolver
                .resolve(allWith(DataStatus.UNVERIFIED)).isEmpty());
    }

    @Test
    @DisplayName("해당 없음(NOT_APPLICABLE)은 확인한 것으로 치지 않는다")
    void resolvesNothingWhenEveryJudgementIsNotApplicable() {
        assertTrue(ChecklistAutoCheckResolver
                .resolve(allWith(DataStatus.NOT_APPLICABLE)).isEmpty());
    }

    // ---------- 핵심 설계 결정 ----------

    @Test
    @DisplayName("DANGER로 판정되면 데이터를 확인했더라도 체크하지 않는다 - 위험한 항목은 사용자가 직접 확인해야 한다")
    void doesNotCheckItemWhenJudgementIsDangerous() {
        RiskEvaluationResultDTO evaluation = evaluation(
                List.of(
                        check(CheckType.MORTGAGE_EXISTENCE, RiskLevel.DANGER, DataStatus.VERIFIED),
                        check(CheckType.RIGHTS_INFRINGEMENT, RiskLevel.DANGER, DataStatus.VERIFIED)
                ),
                List.of()
        );

        assertFalse(contentsOf(ChecklistAutoCheckResolver.resolve(evaluation))
                .contains("등기부등본 확인"));
    }

    @Test
    @DisplayName("CAUTION도 체크하지 않는다 - 자동 체크 기준은 SAFE 하나뿐이다")
    void doesNotCheckItemWhenJudgementIsCaution() {
        RiskEvaluationResultDTO evaluation = evaluation(
                List.of(
                        check(CheckType.MORTGAGE_EXISTENCE, RiskLevel.CAUTION, DataStatus.VERIFIED),
                        check(CheckType.RIGHTS_INFRINGEMENT, RiskLevel.SAFE, DataStatus.VERIFIED)
                ),
                List.of()
        );

        Set<String> contents = contentsOf(ChecklistAutoCheckResolver.resolve(evaluation));
        assertFalse(contents.contains("등기부등본 확인"));
        // 근저당 하나만 근거로 삼는 다세대 '공동근저당'도 같은 이유로 빠진다
        assertFalse(contents.contains("공동근저당"));
    }

    @Test
    @DisplayName("근거가 여러 개인 항목은 하나만 SAFE가 아니어도 체크하지 않는다")
    void requiresEveryUnderlyingJudgementToBeSafe() {
        RiskEvaluationResultDTO evaluation = evaluation(
                List.of(
                        check(CheckType.ILLEGAL_BUILDING, RiskLevel.SAFE, DataStatus.VERIFIED),
                        // 업무용 오피스텔 등 - 확인은 했지만 추가 확인이 필요한 상태
                        check(CheckType.BUILDING_USE, RiskLevel.CAUTION, DataStatus.VERIFIED)
                ),
                List.of()
        );

        Set<String> contents = contentsOf(ChecklistAutoCheckResolver.resolve(evaluation));
        assertFalse(contents.contains("건축물대장 확인"));
        // 위반건축물 하나만 근거로 삼는 항목은 SAFE라 통과한다
        assertTrue(contents.contains("위반건축물"));
    }

    @Test
    @DisplayName("세부 판정(DetailResult)도 SAFE일 때만 체크한다")
    void checksDetailBasedItemOnlyWhenSafe() {
        RiskEvaluationResultDTO dangerous = evaluation(
                List.of(),
                List.of(new DetailResultDTO(
                        DetailType.HIGH_JEONSE_RATIO, RiskLevel.DANGER, DataStatus.VERIFIED))
        );
        assertFalse(contentsOf(ChecklistAutoCheckResolver.resolve(dangerous))
                .contains("전세가율 확인"));

        RiskEvaluationResultDTO safe = evaluation(
                List.of(),
                List.of(new DetailResultDTO(
                        DetailType.HIGH_JEONSE_RATIO, RiskLevel.SAFE, DataStatus.VERIFIED))
        );
        assertTrue(contentsOf(ChecklistAutoCheckResolver.resolve(safe))
                .contains("전세가율 확인"));
    }

    @Test
    @DisplayName("아파트는 유형별 고유 규칙이 없다 - COMMON과 중복이라 V8에서 제거됨")
    void hasNoAutoCheckRuleForApartment() {
        List<VerifiedChecklistItemDTO> result =
                ChecklistAutoCheckResolver.resolve(allWith(DataStatus.VERIFIED));

        assertTrue(result.stream()
                .noneMatch(item -> item.getCategory() == Category.APARTMENT));
    }

    @Test
    @DisplayName("유형별 규칙이 COMMON 규칙과 같은 개념을 덮지 않는다 - 같은 항목이 두 줄로 뜨는 것을 막는다")
    void hasNoTypeRuleDuplicatingCommonRule() {
        List<VerifiedChecklistItemDTO> all =
                ChecklistAutoCheckResolver.resolve(allWith(DataStatus.VERIFIED));

        Set<String> common = all.stream()
                .filter(item -> item.getCategory() == Category.COMMON)
                .map(VerifiedChecklistItemDTO::getContents)
                .collect(Collectors.toSet());
        Set<String> byType = all.stream()
                .filter(item -> item.getCategory() != Category.COMMON)
                .map(VerifiedChecklistItemDTO::getContents)
                .collect(Collectors.toSet());

        assertTrue(common.contains("전세가율 확인"));
        assertTrue(common.contains("등기부등본 확인"));
        assertFalse(byType.contains("전세가율"));
        assertFalse(byType.contains("권리관계 확인"));
    }

    @Test
    @DisplayName("근거가 여러 개인 항목은 하나라도 UNVERIFIED면 체크하지 않는다")
    void requiresEveryUnderlyingJudgementToBeVerified() {
        RiskEvaluationResultDTO evaluation = evaluation(
                List.of(
                        check(CheckType.MORTGAGE_EXISTENCE, RiskLevel.SAFE, DataStatus.VERIFIED),
                        // 등기부 권리침해만 확인 실패
                        check(CheckType.RIGHTS_INFRINGEMENT, RiskLevel.CAUTION, DataStatus.UNVERIFIED)
                ),
                List.of()
        );

        Set<String> contents = contentsOf(ChecklistAutoCheckResolver.resolve(evaluation));
        assertFalse(contents.contains("등기부등본 확인"));
        // 근저당 하나만 필요한 다세대 '공동근저당'은 통과해야 한다
        assertTrue(contents.contains("공동근저당"));
    }

    @Test
    @DisplayName("판정 항목이 아예 누락돼도 체크하지 않는다")
    void doesNotCheckWhenJudgementIsMissingEntirely() {
        RiskEvaluationResultDTO evaluation = evaluation(
                List.of(check(CheckType.MORTGAGE_EXISTENCE, RiskLevel.SAFE, DataStatus.VERIFIED)),
                List.of()
        );

        assertFalse(contentsOf(ChecklistAutoCheckResolver.resolve(evaluation))
                .contains("등기부등본 확인"));
    }

    @Test
    @DisplayName("신탁등기까지 SAFE로 확인해야 등기부등본 항목을 자동 체크한다")
    void registryChecklistAlsoRequiresNoTrustRegistration() {
        RiskEvaluationResultDTO evaluation = evaluation(
                List.of(
                        check(CheckType.MORTGAGE_EXISTENCE, RiskLevel.SAFE, DataStatus.VERIFIED),
                        check(CheckType.RIGHTS_INFRINGEMENT, RiskLevel.SAFE, DataStatus.VERIFIED)
                ),
                List.of(new DetailResultDTO(
                        DetailType.TRUST_REGISTRATION_EXISTENCE,
                        RiskLevel.DANGER,
                        DataStatus.VERIFIED
                ))
        );

        assertFalse(contentsOf(ChecklistAutoCheckResolver.resolve(evaluation))
                .contains("등기부등본 확인"));
    }

    @Test
    @DisplayName("등기상 소유자가 누락되거나 미확인이면 등기부등본을 자동 확인하지 않는다")
    void registryChecklistAlsoRequiresVerifiedRegisteredOwner() {
        RiskEvaluationResultDTO evaluation = evaluation(
                List.of(
                        check(CheckType.MORTGAGE_EXISTENCE, RiskLevel.SAFE, DataStatus.VERIFIED),
                        check(CheckType.RIGHTS_INFRINGEMENT, RiskLevel.SAFE, DataStatus.VERIFIED)
                ),
                List.of(
                        new DetailResultDTO(
                                DetailType.TRUST_REGISTRATION_EXISTENCE,
                                RiskLevel.SAFE,
                                DataStatus.VERIFIED
                        ),
                        new DetailResultDTO(
                                DetailType.REGISTERED_OWNER_VERIFICATION,
                                RiskLevel.CAUTION,
                                DataStatus.UNVERIFIED
                        )
                )
        );

        assertFalse(contentsOf(ChecklistAutoCheckResolver.resolve(evaluation))
                .contains("등기부등본 확인"));
    }

    // ---------- 자동 체크 금지 항목 ----------

    @Test
    @DisplayName("미래 행위·외부 데이터 없는 항목은 전부 VERIFIED여도 절대 체크하지 않는다")
    void neverChecksItemsThatCannotBeVerifiedByAnalysis() {
        Set<String> contents = contentsOf(
                ChecklistAutoCheckResolver.resolve(allWith(DataStatus.VERIFIED))
        );

        assertFalse(contents.contains("잔금 지급 직전 등기부 재확인"));
        assertFalse(contents.contains("잔금 전 등기부 재확인"));
        assertFalse(contents.contains("국세·지방세 체납 확인"));
        assertFalse(contents.contains("국세·지방세 체납"));
        assertFalse(contents.contains("대리계약 여부"));
        assertFalse(contents.contains("신탁회사 동의"));
        assertFalse(contents.contains("전입세대확인서"));
        assertFalse(contents.contains("확정일자 부여현황"));
        assertFalse(contents.contains("선순위 임차인 보증금"));
        assertFalse(contents.contains("HUG/HF/SGI 보증보험 가능 여부 확인"));
    }

    @Test
    @DisplayName("V8에서 제거된 중복 항목은 규칙에도 남아 있지 않다")
    void hasNoRuleForItemsRemovedInV8() {
        Set<String> contents = contentsOf(
                ChecklistAutoCheckResolver.resolve(allWith(DataStatus.VERIFIED))
        );

        // V8 삭제분: OFFICETEL/신탁회사 동의, APARTMENT/권리관계 확인,
        //            APARTMENT/전세가율, MULTI_HOUSEHOLD/전세가율
        assertFalse(contents.contains("권리관계 확인"));
        assertFalse(contents.contains("전세가율"));      // COMMON의 '전세가율 확인'과 다른 문자열
        assertTrue(contents.contains("전세가율 확인"));  // COMMON 쪽은 남아야 한다
    }

    @Test
    @DisplayName("다가구는 자동 체크 대상이 없다")
    void hasNoAutoCheckRuleForMultiFamily() {
        List<VerifiedChecklistItemDTO> result =
                ChecklistAutoCheckResolver.resolve(allWith(DataStatus.VERIFIED));

        assertTrue(result.stream()
                .noneMatch(item -> item.getCategory() == Category.MULTI_FAMILY));
    }

    // ---------- 방어 ----------

    @Test
    @DisplayName("판정 결과가 null이거나 비어 있어도 터지지 않는다")
    void toleratesNullAndEmptyInput() {
        assertTrue(ChecklistAutoCheckResolver.resolve(null).isEmpty());
        assertTrue(ChecklistAutoCheckResolver
                .resolve(new RiskEvaluationResultDTO(RiskLevel.SAFE, null, null))
                .isEmpty());
        assertTrue(ChecklistAutoCheckResolver
                .resolve(new RiskEvaluationResultDTO(RiskLevel.SAFE, List.of(), List.of()))
                .isEmpty());
    }

    @Test
    @DisplayName("같은 (category, contents) 조합이 중복 생성되지 않는다")
    void producesNoDuplicateItems() {
        List<VerifiedChecklistItemDTO> result =
                ChecklistAutoCheckResolver.resolve(allWith(DataStatus.VERIFIED));

        assertEquals(result.size(), Set.copyOf(result).size());
    }
}
