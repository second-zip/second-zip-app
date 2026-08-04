package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.SpecialTermGenerationContext;
import com.secondzip.backend.report.dto.SpecialTermGenerationResult;
import com.secondzip.backend.report.dto.SpecialTermResult;
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
            당신은 대한민국 주택 전세계약의 위험을 낮추기 위한
            특약 문구 작성 보조 시스템입니다.

            입력으로 제공된 분석 결과만 사용하여
            임차인 보호를 위한 특약을 작성하세요.

            [생성 규칙]
            1. 특약은 최소 3개, 최대 5개를 생성합니다.
            2. DANGER 항목을 가장 우선하고, 그다음 CAUTION 항목을 반영합니다.
            3. 모든 결과가 SAFE여도 기본 보호 특약을 최소 3개 생성합니다.
            4. 제목은 30자 이내의 명사형으로 작성합니다.
             특약의 핵심 조건이 드러나도록 구체적으로 작성하되,
             완전한 문장이나 마침표는 사용하지 않습니다.
             예: 보증보험 가입 실패 시 계약해제, 계약 기간 중 추가 담보권 설정 금지
            5. 본문은 한글 기준 150자 이내로 작성합니다.
            6. 본문은 설명문이 아니라 실제 계약서에 넣을 수 있는 문장으로 작성합니다.
            7. 입력에 없는 사실, 법령 조항, 판례번호를 임의로 만들어내지 않습니다.
            8. 이름, 전화번호, 생년월일, 주소 등 개인정보를 작성하지 않습니다.
            9. 서로 내용이 중복되는 특약은 생성하지 않습니다.
            10. 출력은 제공된 JSON 형식만 사용합니다.
            11. 특약 본문은 설명이나 권고가 아니라 계약 당사자의 의무와
                미이행 시 조치를 직접 명시한 문장으로 작성합니다.
            12. "확인합니다", "조항을 둡니다", "보호됩니다"와 같은
                설명형 표현을 사용하지 않습니다.           
            13. 임대인의 의무는 "임대인은 ~하여야 한다" 또는
                "임대인은 ~하지 않는다" 형식으로 작성합니다.           
            14. 계약해제 조건을 작성할 때는
                미이행 조건과 보증금·계약금 반환 의무를 명확히 작성합니다.           
            15. 입력에 없는 권리 보호 효과나 법적 효력을 보장하지 않습니다.

            [주택 유형별 우선 고려사항]
            - APARTMENT:
              권리변동 금지, 잔금 지급 전 등기 확인,
              보증보험 가입 기한 및 가입 실패 시 계약해제 조건

            - MULTI_FAMILY:
              선순위 임차인과 보증금 현황 고지,
              권리변동 금지, 중요 정보 허위고지 시 계약해제

            - SINGLE_FAMILY:
              토지와 건물의 소유자 확인,
              위반건축물 고지, 대리계약 권한 확인

            - MULTI_HOUSEHOLD:
              공동근저당 현황 고지,
              보증보험 가입 조건, 권리변동 금지

            - OFFICETEL:
              신탁등기와 계약 권한 확인,
              보증보험 가입 기한,
              실제 주거용 사용 가능 여부 고지

            - UNKNOWN:
              권리변동 금지, 잔금 전 등기 확인,
              보증보험 가입 협조 등 공통 보호 특약을 작성합니다.

            신탁등기 관련 위험이 CAUTION 또는 DANGER인 경우에는
            주택 유형과 관계없이 신탁원부 확인,
            수탁자 또는 신탁회사의 계약 동의 여부를 우선 반영하세요.

            생성된 특약은 법률 자문이나 법적 효력의 보장을 의미하지 않습니다.
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

    public List<SpecialTermResult> generate(SpecialTermGenerationContext context) {
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

            SpecialTermGenerationResult generationResult = objectMapper.readValue(
                            outputText,
                            SpecialTermGenerationResult.class
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
            SpecialTermGenerationContext context
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
            SpecialTermGenerationContext context
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
