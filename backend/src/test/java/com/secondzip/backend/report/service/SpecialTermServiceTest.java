package com.secondzip.backend.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.SpecialTermGenerationContext;
import com.secondzip.backend.report.dto.SpecialTermResult;
import com.secondzip.backend.report.dto.response.CheckResultView;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.dto.response.SpecialTermView;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.RiskLevel;
import com.secondzip.backend.report.service.external.client.GptSpecialTermGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialTermServiceTest {

    @Test
    void generatesValidatesAndPersistsSpecialTerms() {
        ReportDetailResponse report = report();

        FixedQueryService queryService =
                new FixedQueryService(report);

        CapturingGenerator generator =
                new CapturingGenerator(
                        List.of(
                                new SpecialTermResult(
                                        "  추가 담보권 설정 금지  ",
                                        "  임대인은 잔금 지급 전까지 새로운 담보권을 설정하지 않는다.  "
                                ),
                                new SpecialTermResult(
                                        "잔금 전 등기사항 재확인",
                                        "임대인은 잔금 지급 전 최신 등기사항을 제공하여야 한다."
                                ),
                                new SpecialTermResult(
                                        "보증보험 가입 조건",
                                        "보증보험 가입이 불가능한 경우 임차인은 계약을 해제할 수 있다."
                                )
                        )
                );

        CapturingPersistenceService persistenceService =
                new CapturingPersistenceService();

        SpecialTermService service =
                new SpecialTermService(
                        queryService,
                        new HousingTypeResolver(),
                        generator,
                        new SpecialTermValidator(),
                        persistenceService
                );

        List<SpecialTermView> result =
                service.generateAndSave(1L, 77L);

        // GPT에 전달된 Context 확인
        assertEquals(
                77L,
                generator.context.getAnalysisReportId()
        );

        assertEquals(
                500_000_000L,
                generator.context.getDeposit()
        );

        assertEquals(
                RiskLevel.DANGER,
                generator.context.getOverallRiskLevel()
        );

        // BUILDING_USE evidence를 통해 오피스텔로 판별되는지
        assertEquals(
                "OFFICETEL",
                generator.context.getHousingType()
        );

        // 검증 후 DB 저장 단계까지 진행되었는지
        assertTrue(persistenceService.called);

        assertEquals(
                77L,
                persistenceService.reportId
        );

        assertEquals(
                3,
                persistenceService.terms.size()
        );

        // Validator를 거치며 공백이 제거되었는지 확인
        assertEquals(
                "추가 담보권 설정 금지",
                persistenceService.terms.get(0).getTitle()
        );

        assertEquals(
                "임대인은 잔금 지급 전까지 새로운 담보권을 설정하지 않는다.",
                persistenceService.terms.get(0).getContent()
        );

        assertEquals(3, result.size());
    }

    @Test
    void doesNotPersistWhenGeneratedTermsFailValidation() {
        ReportDetailResponse report = report();

        // GPT가 규칙과 다르게 특약을 2개만 반환한 상황
        CapturingGenerator generator =
                new CapturingGenerator(
                        List.of(
                                new SpecialTermResult("특약1", "내용1"),
                                new SpecialTermResult("특약2", "내용2")
                        )
                );

        CapturingPersistenceService persistenceService =
                new CapturingPersistenceService();

        SpecialTermService service =
                new SpecialTermService(
                        new FixedQueryService(report),
                        new HousingTypeResolver(),
                        generator,
                        new SpecialTermValidator(),
                        persistenceService
                );

        assertThrows(
                BusinessException.class,
                () -> service.generateAndSave(1L, 77L)
        );

        // 검증 실패한 AI 응답은 DB 저장 단계로 넘어가면 안 됨
        assertFalse(persistenceService.called);
    }

    @Test
    void doesNotReplaceExistingTermsWhenGptGenerationFails() {
        ReportDetailResponse report = report();

        FailingGenerator generator =
                new FailingGenerator();

        CapturingPersistenceService persistenceService =
                new CapturingPersistenceService();

        SpecialTermService service =
                new SpecialTermService(
                        new FixedQueryService(report),
                        new HousingTypeResolver(),
                        generator,
                        new SpecialTermValidator(),
                        persistenceService
                );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.generateAndSave(1L, 77L)
        );

        assertEquals(
                ErrorCode.EXTERNAL_API_ERROR,
                exception.getErrorCode()
        );

        // GPT 호출 실패 시 replace 자체가 실행되지 않음
        // → 기존 특약 delete도 발생하지 않음
        assertFalse(persistenceService.called);
    }

    private ReportDetailResponse report() {

        CheckResultView buildingUse =
                new CheckResultView(
                        CheckType.BUILDING_USE,
                        RiskLevel.SAFE,
                        DataStatus.VERIFIED,
                        Map.of(
                                "buildingUse",
                                "업무시설(오피스텔)"
                        )
                );

        return new ReportDetailResponse(
                77L,
                "서울특별시 강남구 테헤란로 152",
                "101동 1203호",
                500_000_000L,
                RiskLevel.DANGER,
                false,
                "OFFICETEL",
                false,
                List.of(buildingUse),
                List.of(),
                List.of()
        );
    }

    private static class FixedQueryService
            extends ReportQueryService {

        private final ReportDetailResponse response;

        private FixedQueryService(
                ReportDetailResponse response
        ) {
            super(null, new ObjectMapper());
            this.response = response;
        }

        @Override
        public ReportDetailResponse getReportDetail(
                Long accountId,
                Long reportId
        ) {
            return response;
        }
    }

    private static class CapturingGenerator
            extends GptSpecialTermGenerator {

        private final List<SpecialTermResult> result;

        private SpecialTermGenerationContext context;

        private CapturingGenerator(
                List<SpecialTermResult> result
        ) {
            super(
                    new RestTemplate(),
                    new ObjectMapper()
            );

            this.result = result;
        }

        @Override
        public List<SpecialTermResult> generate(
                SpecialTermGenerationContext context
        ) {
            this.context = context;
            return result;
        }
    }

    private static class FailingGenerator
            extends GptSpecialTermGenerator {

        private FailingGenerator() {
            super(
                    new RestTemplate(),
                    new ObjectMapper()
            );
        }

        @Override
        public List<SpecialTermResult> generate(
                SpecialTermGenerationContext context
        ) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OpenAI API 호출에 실패했습니다."
            );
        }
    }

    private static class CapturingPersistenceService
            extends SpecialTermPersistenceService {

        private boolean called;

        private Long reportId;

        private List<SpecialTermResult> terms =
                List.of();

        private CapturingPersistenceService() {
            super(null);
        }

        @Override
        public List<SpecialTermView> replace(
                Long reportId,
                List<SpecialTermResult> terms
        ) {
            this.called = true;
            this.reportId = reportId;
            this.terms = List.copyOf(terms);

            List<SpecialTermView> result =
                    new ArrayList<>();

            for (int i = 0; i < terms.size(); i++) {
                SpecialTermResult term = terms.get(i);

                result.add(
                        new SpecialTermView(
                                i + 1,
                                term.getTitle(),
                                term.getContent()
                        )
                );
            }

            return result;
        }
    }
}