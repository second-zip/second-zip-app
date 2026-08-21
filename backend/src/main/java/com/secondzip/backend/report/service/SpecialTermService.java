package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.SpecialTermGenerationContextDTO;
import com.secondzip.backend.report.dto.SpecialTermResultDTO;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.dto.response.SpecialTermView;
import com.secondzip.backend.report.service.external.client.GptSpecialTermGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SpecialTermService {

    private static final Set<String> SUPPORTED_HOUSING_TYPES = Set.of(
            "SINGLE_FAMILY", "MULTI_FAMILY", "APARTMENT",
            "MULTI_HOUSEHOLD", "OFFICETEL"
    );

    private final ReportQueryService reportQueryService;
    private final HousingTypeResolver housingTypeResolver;
    private final GptSpecialTermGenerator gptSpecialTermGenerator;
    private final SpecialTermValidator specialTermValidator;
    private final SpecialTermPersistenceService specialTermPersistenceService;

    public List<SpecialTermView> generateAndSave(Long accountId, Long reportId
    ) {
        // 1. 소유권 검증 후 리포트 분석 결과 조회
        ReportDetailResponse report = reportQueryService.getReportDetail(accountId, reportId);

        // 2. 준비 단계에서 확정·저장한 유형을 그대로 쓴다.
        // 구버전 리포트처럼 값이 없을 때만 BUILDING_USE 근거로 폴백한다.
        String housingType = report.getHousingCategory();
        if (housingType == null || !SUPPORTED_HOUSING_TYPES.contains(housingType)) {
            housingType = housingTypeResolver.resolve(report.getCheckResults());
        }

        // 3. GPT에 전달할 입력 구성
        SpecialTermGenerationContextDTO context =
                new SpecialTermGenerationContextDTO(
                        report.getAnalysisReportId(),
                        report.getDeposit(),
                        report.getResult(),
                        housingType,
                        report.getCheckResults(),
                        report.getFraudTypes()
                );

        // 4. 실제 GPT 호출
        List<SpecialTermResultDTO> generatedTerms = gptSpecialTermGenerator.generate(context);

        // 5. 개수·길이·중복 검증
        List<SpecialTermResultDTO> validatedTerms = specialTermValidator.validateAndNormalize(generatedTerms);

        // 6. 기존 특약을 새 특약으로 교체
        return specialTermPersistenceService.replace(reportId, validatedTerms);
    }
}
