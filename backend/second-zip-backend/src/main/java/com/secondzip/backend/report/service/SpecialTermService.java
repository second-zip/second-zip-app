package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.SpecialTermGenerationContext;
import com.secondzip.backend.report.dto.SpecialTermResult;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.dto.response.SpecialTermView;
import com.secondzip.backend.report.service.external.client.GptSpecialTermGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialTermService {

    private final ReportQueryService reportQueryService;
    private final HousingTypeResolver housingTypeResolver;
    private final GptSpecialTermGenerator gptSpecialTermGenerator;
    private final SpecialTermValidator specialTermValidator;
    private final SpecialTermPersistenceService specialTermPersistenceService;

    public List<SpecialTermView> generateAndSave(Long accountId, Long reportId
    ) {
        // 1. 소유권 검증 후 리포트 분석 결과 조회
        ReportDetailResponse report = reportQueryService.getReportDetail(accountId, reportId);

        // 2. BUILDING_USE evidence에서 주택 유형 판별
        String housingType = housingTypeResolver.resolve(report.getCheckResults());

        // 3. GPT에 전달할 입력 구성
        SpecialTermGenerationContext context =
                new SpecialTermGenerationContext(
                        report.getAnalysisReportId(),
                        report.getDeposit(),
                        report.getResult(),
                        housingType,
                        report.getCheckResults(),
                        report.getFraudTypes()
                );

        // 4. 실제 GPT 호출
        List<SpecialTermResult> generatedTerms = gptSpecialTermGenerator.generate(context);

        // 5. 개수·길이·중복 검증
        List<SpecialTermResult> validatedTerms = specialTermValidator.validateAndNormalize(generatedTerms);

        // 6. 기존 특약을 새 특약으로 교체
        return specialTermPersistenceService.replace(reportId, validatedTerms);
    }
}