package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.response.CheckResultView;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class HousingTypeResolverTest {

    private final HousingTypeResolver resolver = new HousingTypeResolver();

    @Test
    @DisplayName("점검 결과가 없으면 주택 유형을 UNKNOWN으로 반환한다")
    void returnsUnknownForMissingResults() {
        assertThat(resolver.resolve(null)).isEqualTo("UNKNOWN");
        assertThat(resolver.resolve(List.of())).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("무관하거나 근거가 없는 결과를 건너뛰고 건축물 용도 근거를 찾는다")
    void skipsIrrelevantAndMissingEvidence() {
        List<CheckResultView> results = Arrays.asList(
                null,
                result(CheckType.MORTGAGE_EXISTENCE, Map.of(
                        "buildingUse", "오피스텔"
                )),
                result(CheckType.BUILDING_USE, null),
                result(CheckType.BUILDING_USE, Map.of()),
                result(
                        CheckType.BUILDING_USE,
                        Collections.singletonMap("buildingUse", null)
                ),
                result(CheckType.BUILDING_USE, Map.of(
                        "buildingUse", "계단실"
                )),
                result(CheckType.BUILDING_USE, Map.of(
                        "buildingUse", "공동주택 아파트"
                ))
        );

        assertThat(resolver.resolve(results)).isEqualTo("APARTMENT");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("buildingUseCases")
    @DisplayName("건축물 용도 문자열을 체크리스트 주택 유형으로 변환한다")
    void resolvesHousingCategory(String buildingUse, String expected) {
        CheckResultView checkResult = result(
                CheckType.BUILDING_USE,
                Map.of("buildingUse", buildingUse)
        );

        assertThat(resolver.resolve(List.of(checkResult)))
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("건축물 용도 근거가 공백이거나 알 수 없는 값이면 UNKNOWN이다")
    void returnsUnknownForBlankOrUnsupportedUse() {
        assertThat(resolver.resolve(List.of(result(
                CheckType.BUILDING_USE,
                Map.of("buildingUse", "   ")
        )))).isEqualTo("UNKNOWN");
        assertThat(resolver.resolve(List.of(result(
                CheckType.BUILDING_USE,
                Map.of("buildingUse", 12345)
        )))).isEqualTo("UNKNOWN");
    }

    private static Stream<Arguments> buildingUseCases() {
        return Stream.of(
                Arguments.of("업무용 오피스텔", "OFFICETEL"),
                Arguments.of("공동주택(아파트)", "APARTMENT"),
                Arguments.of("다 세 대 주 택", "MULTI_HOUSEHOLD"),
                Arguments.of("연립주택", "MULTI_HOUSEHOLD"),
                Arguments.of("빌라", "MULTI_HOUSEHOLD"),
                Arguments.of("다가구주택", "MULTI_FAMILY"),
                Arguments.of("단독 주택", "SINGLE_FAMILY"),
                Arguments.of("단독주택, 다가구주택, 행복빌라", "MULTI_FAMILY"),
                Arguments.of("공동주택, 다세대주택, 샹떼빌아파트", "MULTI_HOUSEHOLD"),
                Arguments.of("근린생활시설", "UNKNOWN"),
                Arguments.of("아파트형 오피스텔", "OFFICETEL")
        );
    }

    private CheckResultView result(
            CheckType checkType,
            Map<String, Object> evidence
    ) {
        return new CheckResultView(
                checkType,
                RiskLevel.SAFE,
                DataStatus.VERIFIED,
                evidence
        );
    }
}
