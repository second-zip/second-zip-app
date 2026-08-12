package com.secondzip.backend.report.service.external.mock;

import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import com.secondzip.backend.report.dto.request.StartAnalysisAuthRequest;
import com.secondzip.backend.report.dto.request.ContinueAnalysisAuthRequest;
import com.secondzip.backend.report.enums.AnalysisNextAction;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.report.service.external.MockApiCondition;
import com.secondzip.backend.report.service.external.client.BuildingRegisterGateway;
import com.secondzip.backend.report.service.external.client.BuildingRegisterGatewayResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 건축물대장 간편인증(카카오톡 등)을 흉내내는 Mock 게이트웨이.
 * 실제 CODEF 응답과 같은 필드명(resViolationStatus/resUseType/resBasePrice)을
 * 그대로 사용해서, BuildingRegisterDataParser 파싱 로직도 mock 모드에서 같이 검증되게 한다.
 */
@Slf4j
@Component
@Conditional(MockApiCondition.class)
public class MockBuildingRegisterGateway implements BuildingRegisterGateway {

    @Override
    public BuildingRegisterGatewayResult start(
            AnalysisWorkflowState state,
            BuildingRegisterDocumentType documentType,
            StartAnalysisAuthRequest authRequest
    ) {
        log.info("[MOCK] 건축물대장 간편인증 즉시 완료 처리: documentType={}, userName={}",
                documentType, authRequest.getUserName());
        return completedResult();
    }

    @Override
    public BuildingRegisterGatewayResult continueRequest(
            AnalysisWorkflowState state,
            ContinueAnalysisAuthRequest request
    ) {
        // mock은 항상 1차 요청에서 completed=true로 끝나므로 호출될 일이 없지만 방어적으로 처리
        return completedResult();
    }

    private BuildingRegisterGatewayResult completedResult() {
        Map<String, Object> data = Map.of(
                "resViolationStatus", "",              // 빈 값 = 위반건축물 아님
                "resUseType", "공동주택",
                "resBasePrice", "850000000"             // 공시가격 목업값
        );
        return new BuildingRegisterGatewayResult(
                true,
                AnalysisNextAction.NONE,
                null,
                List.of(),
                null,
                data
        );
    }
}
