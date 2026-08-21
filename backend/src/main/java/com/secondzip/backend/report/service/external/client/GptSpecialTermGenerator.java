package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.SpecialTermGenerationContextDTO;
import com.secondzip.backend.report.dto.SpecialTermGenerationResultDTO;
import com.secondzip.backend.report.dto.SpecialTermResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GptSpecialTermGenerator {

    private static final String OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";

    private static final int MAX_OUTPUT_TOKENS = 2000;

    private static final String SYSTEM_PROMPT = """
        당신은 대한민국 주택 임대차계약의 위험을 줄이기 위한
        임차인 보호 특약 문구 작성 보조 시스템입니다.

        입력으로 제공된 분석 결과만을 근거로,
        실제 임대차계약서의 특약사항란에 바로 사용할 수 있는 형태의
        특약 문구를 작성하세요.

        [1. 생성 원칙]
        - 특약은 최소 3개, 최대 5개를 생성합니다.
        - DANGER 항목을 가장 우선하고, 그다음 CAUTION 항목을 반영합니다.
        - SAFE 항목은 원칙적으로 별도의 위험 특약 생성 근거로 사용하지 않습니다.
        - 모든 분석 결과가 SAFE인 경우에는
          임차인 보호를 위한 일반적인 기본 특약을 최소 3개 생성합니다.
        - 서로 동일하거나 실질적으로 중복되는 특약은 생성하지 않습니다.

        [2. 내부 분석값 처리]
        - 입력에 포함된 영문 enum, 코드명, boolean 값은
          내부 판단에만 사용하고 결과에 그대로 출력하지 않습니다.
        - DANGER, CAUTION, SAFE, TRUE, FALSE, UNKNOWN, NULL 등의 값을
          제목이나 본문에 절대 노출하지 않습니다.
        - TRUST_REGISTRATION_EXISTENCE와 같은 개발용 식별자도
          결과에 그대로 출력하지 않습니다.
        - 내부 코드의 의미가 필요한 경우 반드시 자연스러운 한국어로 해석합니다.

        예:
        TRUST_REGISTRATION_EXISTENCE → 신탁등기 여부
        BUILDING_USE → 건축물 용도
        FALSE → 해당 위험이 확인되지 않음
        TRUE → 해당 사항이 확인됨

        단, 위 변환 예시 문구 자체를 기계적으로 출력하지 말고
        실제 계약 내용에 자연스럽게 반영하세요.

        [3. 제목 작성 규칙]
        - 제목은 30자 이내의 명사형으로 작성합니다.
        - 제목만 읽어도 특약의 핵심 조건을 알 수 있어야 합니다.
        - 문장형 표현이나 마침표는 사용하지 않습니다.
        - 위험등급, 번호, 내부 코드명을 제목으로 사용하지 않습니다.

        좋은 예:
        - 추가 담보권 설정 금지
        - 잔금 전 등기사항 재확인
        - 보증보험 가입 실패 시 계약해제
        - 대리계약 권한 증빙 의무
        - 신탁계약 권한 확인

        나쁜 예:
        - DANGER 특약
        - CAUTION 특약 1
        - TRUST 관련 특약
        - 임차인을 보호하기 위한 특약

        [4. 본문 작성 규칙]
        - 본문은 한글 기준 180자 이내로 작성합니다.
        - 설명, 권고, 해설이 아니라
          실제 계약서 특약란에 넣을 수 있는 하나의 완결된 문장으로 작성합니다.
        - 누가 무엇을 해야 하는지 의무의 주체를 명확하게 작성합니다.
        - 임대인의 의무는 가능한 경우
          "임대인은 ~하여야 한다."
          또는
          "임대인은 ~하지 않는다."
          형식으로 작성합니다.
        - 임차인의 계약해제 조건이 필요한 경우
          어떤 상황에서 계약을 해제할 수 있는지 명확히 작성합니다.
        - 계약금 또는 보증금 반환이 필요한 경우
          반환 주체와 반환 의무를 명확하게 작성합니다.
        - 한 특약에는 서로 밀접하게 관련된 하나의 보호 목적만 담습니다.

        좋은 예:
        "임대인은 잔금 지급 전까지 해당 주택에 새로운 근저당권 등
        임차인의 권리에 영향을 줄 수 있는 권리를 설정하지 않는다."

        좋은 예:
        "임대인이 계약 체결 후 새로운 담보권을 설정한 경우
        임차인은 계약을 해제할 수 있으며,
        임대인은 지급받은 계약금과 보증금을 반환하여야 한다."

        [5. 금지사항]
        - 입력에 없는 사실을 임의로 추가하지 않습니다.
        - 입력에 없는 계약 유형, 거래 방식, 예치 방식 등을 만들어내지 않습니다.
        - 확인되지 않은 법령 조항이나 판례번호를 작성하지 않습니다.
        - 법적 효력이나 권리 보호가 반드시 보장된다고 단정하지 않습니다.
        - 손해배상이 자동으로 인정된다고 단정하지 않습니다.
        - 이름, 전화번호, 생년월일, 상세 주소 등 개인정보를 작성하지 않습니다.
        - 주택 임대차를 매매계약처럼 표현하지 않습니다.
        - "등기이전", "소유권 이전" 등 매매를 전제로 한 표현을
          임대차 특약에 임의로 사용하지 않습니다.
        - "확인합니다", "권고합니다", "주의가 필요합니다",
          "조항을 둡니다", "보호됩니다"와 같은 설명형 표현을 사용하지 않습니다.
        - "위험에 대비하여", "우려가 있으므로" 등
          분석 설명을 계약 문구에 포함하지 않습니다.

        [6. 주택 유형별 참고사항]
        아래 항목은 해당 주택 유형이면서,
        입력된 분석 결과에서 관련 위험이 실제로 확인된 경우에만 참고합니다.
        주택 유형만을 이유로 SAFE인 항목에 대한 특약을 만들지 않습니다.

        - APARTMENT
          추가 권리변동 방지
          잔금 지급 전 등기사항 확인
          보증보험 가입 조건

        - MULTI_FAMILY
          선순위 임차인 및 선순위 보증금 현황
          추가 권리변동 방지
          중요 정보 허위고지 시 계약해제 조건

        - SINGLE_FAMILY
          토지와 건물의 소유관계
          위반건축물 관련 고지
          대리계약 권한 확인

        - MULTI_HOUSEHOLD
          공동담보 및 근저당 현황
          보증보험 가입 조건
          추가 권리변동 방지

        - OFFICETEL
          신탁등기 및 계약 권한
          보증보험 가입 조건
          실제 주거용 사용 가능 여부

        - UNKNOWN
          주택 유형을 임의로 추정하지 않습니다.
          입력된 위험 항목과 일반적인 임차인 보호 내용만 반영합니다.

        [7. 신탁 관련 특약]
        - 신탁등기 관련 분석 결과가 CAUTION 또는 DANGER인 경우에만
          신탁 관련 특약을 생성합니다.
        - SAFE인 경우에는 신탁 관련 특약을 생성하지 않습니다.
        - 필요한 경우 신탁원부 확인,
          임대차계약 체결 권한,
          수탁자 또는 신탁회사의 동의 여부 등을 반영할 수 있습니다.
        - "TRUST", "TRUST 등록", "TRUST_REGISTRATION_EXISTENCE"와 같은
          내부 표현은 절대 출력하지 않습니다.
        - 반드시 "신탁등기", "신탁원부", "계약 권한" 등
          일반 사용자가 이해할 수 있는 한국어 표현을 사용합니다.

        [8. 출력 형식]
        - 반드시 제공된 JSON 형식으로만 출력합니다.
        - JSON 외의 설명, 주의사항, 마크다운, 분석 과정은 출력하지 않습니다.
        - 각 특약에는 title과 content만 포함합니다.
        - 분석에 사용한 위험등급이나 내부 코드값을 별도 표시하지 않습니다.

        최종 결과를 출력하기 전에 각 특약을 스스로 확인하세요.
        1. 실제 임대차계약서에 그대로 넣을 수 있는 문장인가?
        2. 내부 코드나 위험등급이 노출되어 있지 않은가?
        3. 입력에 없는 사실을 추가하지 않았는가?
        4. 의무의 주체가 명확한가?
        5. 다른 특약과 내용이 중복되지 않는가?

        생성된 특약은 입력된 분석 결과를 기반으로 작성하는
        임차인 보호용 계약 특약 초안입니다.
        """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${GPT_API_KEY:}")
    private String apiKey;

    @Value("${GPT_MODEL:gpt-5-nano}")
    private String model;

    public GptSpecialTermGenerator(
            @Qualifier("gptRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<SpecialTermResultDTO> generate(SpecialTermGenerationContextDTO context) {
        validateConfiguration();

        try {
            Map<String, Object> requestBody = buildRequestBody(context);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info(
                    "OpenAI 특약 생성 요청. reportId={}, model={}",
                    context.getAnalysisReportId(),
                    model
            );

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                            OPENAI_RESPONSES_URL,
                            request,
                            JsonNode.class
                    );

            JsonNode responseBody = response.getBody();

            if (responseBody == null) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "OpenAI 응답 본문이 비어 있습니다."
                );
            }

            String outputText = extractOutputText(responseBody);

            SpecialTermGenerationResultDTO generationResult = objectMapper.readValue(
                            outputText,
                            SpecialTermGenerationResultDTO.class
                    );

            if (generationResult == null || generationResult.getSpecialTerms() == null) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "OpenAI 특약 응답 형식이 올바르지 않습니다."
                );
            }

            log.info(
                    "OpenAI 특약 생성 완료. reportId={}, count={}",
                    context.getAnalysisReportId(),
                    generationResult.getSpecialTerms().size()
            );

            return generationResult.getSpecialTerms();

        } catch (BusinessException e) {
            throw e;

        } catch (HttpStatusCodeException e) {
            log.warn(
                    "OpenAI API 호출 실패. reportId={}, status={}",
                    context.getAnalysisReportId(),
                    e.getStatusCode()
            );

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OpenAI API 호출에 실패했습니다."
            );

        } catch (JsonProcessingException e) {
            log.warn(
                    "OpenAI 응답 JSON 파싱 실패. reportId={}",
                    context.getAnalysisReportId(),
                    e
            );

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OpenAI 응답을 해석할 수 없습니다."
            );

        } catch (RestClientException e) {
            log.warn(
                    "OpenAI API 통신 실패. reportId={}",
                    context.getAnalysisReportId(),
                    e
            );

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OpenAI API 통신 중 오류가 발생했습니다."
            );
        }
    }

    private void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "GPT_API_KEY 설정이 누락되었습니다."
            );
        }

        if (model == null || model.isBlank()) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "GPT_MODEL 설정이 누락되었습니다."
            );
        }
    }

    private Map<String, Object> buildRequestBody(
            SpecialTermGenerationContextDTO context
    ) {
        Map<String, Object> requestBody =
                new LinkedHashMap<>();

        requestBody.put("model", model);

        requestBody.put(
                "reasoning",
                Map.of(
                        "effort", "minimal"
                )
        );

        requestBody.put(
                "input",
                List.of(
                        Map.of(
                                "role", "system",
                                "content", SYSTEM_PROMPT
                        ),
                        Map.of(
                                "role", "user",
                                "content", buildUserPrompt(context)
                        )
                )
        );

        requestBody.put(
                "max_output_tokens",
                MAX_OUTPUT_TOKENS
        );

        requestBody.put(
                "text",
                Map.of(
                        "format",
                        buildResponseFormat()
                )
        );

        return requestBody;
    }

    private String buildUserPrompt(
            SpecialTermGenerationContextDTO context
    ) {
        Map<String, Object> analysisData = new LinkedHashMap<>();

        analysisData.put(
                "deposit", context.getDeposit()
        );
        analysisData.put(
                "overallRiskLevel", context.getOverallRiskLevel()
        );
        analysisData.put(
                "housingType", context.getHousingType()
        );
        analysisData.put(
                "checkResults", context.getCheckResults()
        );
        analysisData.put(
                "fraudTypes", context.getFraudTypes()
        );

        try {
            String analysisJson = objectMapper.writeValueAsString(analysisData);

            return """
                    다음 전세 위험 분석 결과를 바탕으로
                    임차인을 보호할 수 있는 특약을 작성하세요.

                    분석 결과:
                    %s
                    """.formatted(analysisJson);

        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "특약 생성 입력을 구성할 수 없습니다."
            );
        }
    }

    private Map<String, Object> buildResponseFormat() {
        Map<String, Object> termProperties = new LinkedHashMap<>();

        termProperties.put(
                "title",
                Map.of(
                        "type", "string",
                        "description", "특약의 핵심 조건이 드러나는 30자 이내의 명사형 제목"
                )
        );

        termProperties.put(
                "content",
                Map.of(
                        "type", "string",
                        "description", "200자 이내의 계약서용 특약 문구"
                )
        );

        Map<String, Object> termItem = new LinkedHashMap<>();

        termItem.put("type", "object");
        termItem.put("properties", termProperties);
        termItem.put(
                "required",
                List.of("title", "content")
        );
        termItem.put("additionalProperties", false);

        Map<String, Object> specialTermsArray = new LinkedHashMap<>();

        specialTermsArray.put("type", "array");
        specialTermsArray.put("minItems", 3);
        specialTermsArray.put("maxItems", 5);
        specialTermsArray.put("items", termItem);

        Map<String, Object> rootProperties = new LinkedHashMap<>();

        rootProperties.put(
                "specialTerms",
                specialTermsArray
        );

        Map<String, Object> schema = new LinkedHashMap<>();

        schema.put("type", "object");
        schema.put("properties", rootProperties);
        schema.put(
                "required",
                List.of("specialTerms")
        );
        schema.put("additionalProperties", false);

        Map<String, Object> format = new LinkedHashMap<>();

        format.put("type", "json_schema");
        format.put("name", "special_terms");
        format.put("strict", true);
        format.put("schema", schema);

        return format;
    }

    private String extractOutputText(JsonNode responseBody) {
        String status = responseBody.path("status").asText();

        if ("incomplete".equals(status)) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OpenAI 응답 생성이 완료되지 않았습니다."
            );
        }

        JsonNode outputItems = responseBody.path("output");

        if (!outputItems.isArray()) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OpenAI 응답에 output 배열이 없습니다."
            );
        }

        for (JsonNode outputItem : outputItems) {
            if (!"message".equals(
                    outputItem.path("type").asText()
            )) {
                continue;
            }

            JsonNode contents = outputItem.path("content");

            if (!contents.isArray()) {
                continue;
            }

            for (JsonNode content : contents) {
                String contentType = content.path("type").asText();

                if ("refusal".equals(contentType)) {
                    throw new BusinessException(
                            ErrorCode.EXTERNAL_API_ERROR,
                            "OpenAI가 특약 생성을 거부했습니다."
                    );
                }

                if ("output_text".equals(contentType)) {
                    String text = content.path("text").asText();

                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
        }

        throw new BusinessException(
                ErrorCode.EXTERNAL_API_ERROR,
                "OpenAI 응답에서 특약 내용을 찾을 수 없습니다."
        );
    }
}
