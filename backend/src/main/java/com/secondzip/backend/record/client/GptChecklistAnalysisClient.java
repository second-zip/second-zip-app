package com.secondzip.backend.record.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.record.dto.request.ChecklistItemInput;
import com.secondzip.backend.record.dto.response.ChecklistAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GptChecklistAnalysisClient
        implements ChecklistAnalysisClient {

    private static final String OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";
    private static final int MAX_OUTPUT_TOKENS = 4000;
    private static final String SYSTEM_PROMPT = """
            당신은 대한민국 주택 임대차 계약 과정의
            체크리스트 확인 여부를 분석하는 시스템입니다.

            사용자와 임대인 또는 중개인 간의 대화 녹취문과
            확인해야 할 체크리스트 목록이 입력으로 제공됩니다.

            각 체크리스트 항목에 대해
            녹취문에 실제 확인 근거가 존재하는지 판단하세요.

            [상태 판단 기준]

            CHECKED
            - 해당 체크리스트 항목이 실제로 확인되었다는
              명확한 근거가 녹취문에 있는 경우
            - 단순히 관련 단어가 등장한 것만으로는 CHECKED 처리하지 않습니다.

            UNCHECKED
            - 해당 항목이 녹취문에서 전혀 언급되지 않은 경우
            - 확인이 아직 이루어지지 않은 경우
            - "확인해야 합니다"
            - "확인할 예정입니다"
            - "나중에 확인하겠습니다"
            - "추가 확인이 필요합니다"
              등의 표현은 확인 완료가 아니므로 CHECKED 처리하지 않습니다.

            NEEDS_REVIEW
            - 관련 내용은 언급되었지만 실제 확인 완료 여부가 불명확한 경우
            - 서로 상충되는 내용이 존재하는 경우
            - 녹취만으로 확정적인 판단이 어려운 경우

            PROVISIONAL
            - 현재 분석에서는 사용하지 않습니다.
            - 반드시 CHECKED, UNCHECKED, NEEDS_REVIEW 중 하나를 반환하세요.
            
            [확인 완료 판단 보완 규칙]
            
            - "확인했다"라는 단어가 직접 등장하지 않아도,
              체크리스트 항목에 대한 구체적인 결과나 수치가 제시되었다면
              해당 정보를 확인한 것으로 판단할 수 있습니다.
            
            예:
            - "전세가율은 약 70%입니다."
              → 전세가율 확인: CHECKED
            
            - "국세와 지방세 체납은 없습니다."
              → 국세·지방세 체납 확인: CHECKED
            
            - "근저당권은 없습니다."
              → 관련 권리관계 확인 항목의 근거가 될 수 있음
            
            단, 단순 추측이나 예정 표현은 CHECKED가 아닙니다.
            
            예:
            - "전세가율이 70% 정도일 것 같습니다."
              → NEEDS_REVIEW
            
            - "전세가율은 나중에 확인하겠습니다."
              → UNCHECKED

            [중요 규칙]

            1. 입력으로 제공된 checklistItemId를 절대 변경하지 않습니다.

            2. 입력으로 제공된 모든 체크리스트에 대해
               반드시 정확히 한 개의 결과를 반환합니다.

            3. 입력에 존재하지 않는 checklistItemId를
               임의로 생성하지 않습니다.

            4. 같은 checklistItemId를 중복 반환하지 않습니다.

            5. confidenceScore는 0 이상 1 이하의 숫자로 반환합니다.

            6. evidenceText는 CHECKED 또는 NEEDS_REVIEW 판단의
               근거가 되는 실제 녹취문의 문장을 사용합니다.

            7. 해당 내용이 녹취문에 존재하지 않는 경우
               evidenceText는 빈 문자열로 반환합니다.

            8. reason은 해당 상태로 판단한 이유를
               짧고 명확하게 한국어로 작성합니다.

            9. 추측하거나 녹취문에 없는 사실을 추가하지 않습니다.

            10. 법률적 사실이나 행정기관 확인이 실제로 완료됐는지를
                대화만으로 임의 추론하지 않습니다.

            11. "없다"고 말한 사실이 특정 체크리스트의 확인 결과와
                직접 관련된다면 근거로 사용할 수 있습니다.

            12. 서로 비슷한 체크리스트라도 각각 독립적으로 판단합니다.

            [예시]

            녹취:
            "임대인은 등기부 등본을 확인했다고 말했습니다.
             국세와 지방세 체납도 없다고 했습니다.
             전세 보증보험 가입 가능 여부는 추가로 확인해야 합니다."

            판단 예시:

            등기부등본 확인
            → CHECKED

            국세·지방세 체납 확인
            → CHECKED

            보증보험 가능 여부 확인
            → UNCHECKED 또는 NEEDS_REVIEW

            건축물대장 확인
            → UNCHECKED

            출력은 반드시 제공된 JSON Schema 형식만 사용하세요.
            """;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${GPT_API_KEY}")
    private String apiKey;


    @Value("${GPT_MODEL}")
    private String model;

    @Override
    public ChecklistAnalysisResult analyze(
            String transcript,
            List<ChecklistItemInput> checklistItems
    ) {
        validateConfiguration(
                transcript,
                checklistItems
        );
        try {

            Map<String, Object> requestBody =
                    buildRequestBody(
                            transcript,
                            checklistItems
                    );


            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            headers.setBearerAuth(
                    apiKey
            );


            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(
                            requestBody,
                            headers
                    );


            log.info(
                    "OpenAI 체크리스트 분석 요청. itemCount={}, model={}",
                    checklistItems.size(),
                    model
            );


            ResponseEntity<JsonNode> response = //JSON 전체 구조를 트리 형태로 자유롭게 탐색
                    restTemplate.postForEntity(
                            OPENAI_RESPONSES_URL,
                            request,
                            JsonNode.class
                    );


            JsonNode responseBody =
                    response.getBody(); //OpenAI 응답 JSON 전체


            if (responseBody == null) {

                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "OpenAI 응답 본문이 비어 있습니다."
                );
            }


            String outputText =
                    extractOutputText(
                            responseBody //실제 AI 출력 문자열 추출
                    );


            ChecklistAnalysisResult result =
                    objectMapper.readValue(
                            outputText,
                            ChecklistAnalysisResult.class
                    );


            if (result == null
                    || result.getResults() == null) {

                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "OpenAI 체크리스트 분석 응답 형식이 올바르지 않습니다."
                );
            }


            log.info(
                    "OpenAI 체크리스트 분석 완료. resultCount={}",
                    result.getResults().size()
            );


            return result;

        } catch (BusinessException e) {

            throw e;

        } catch (HttpStatusCodeException e) {

            log.warn(
                    "OpenAI API 호출 실패. status={}",
                    e.getStatusCode()
            );


            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OpenAI API 호출에 실패했습니다."
            );

        } catch (JsonProcessingException e) {

            log.warn(
                    "OpenAI 체크리스트 응답 JSON 파싱 실패.",
                    e
            );


            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OpenAI 체크리스트 응답을 해석할 수 없습니다."
            );

        } catch (RestClientException e) {

            log.warn(
                    "OpenAI API 통신 실패.",
                    e
            );


            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OpenAI API 통신 중 오류가 발생했습니다."
            );
        }
    }


    private void validateConfiguration(
            String transcript,
            List<ChecklistItemInput> checklistItems
    ) {

        if (apiKey == null
                || apiKey.isBlank()) {

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "GPT_API_KEY 설정이 누락되었습니다."
            );
        }


        if (model == null
                || model.isBlank()) {

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "GPT_MODEL 설정이 누락되었습니다."
            );
        }


        if (transcript == null
                || transcript.isBlank()) {

            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "AI가 분석할 녹취문이 없습니다."
            );
        }


        if (checklistItems == null
                || checklistItems.isEmpty()) {

            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "AI가 분석할 체크리스트가 없습니다."
            );
        }
    }


    private Map<String, Object> buildRequestBody(
            String transcript,
            List<ChecklistItemInput> checklistItems
    ) {

        Map<String, Object> requestBody =
                new LinkedHashMap<>();


        requestBody.put(
                "model",
                model
        );


        requestBody.put(
                "reasoning",
                Map.of(
                        "effort",
                        "minimal"
                )
        );


        requestBody.put(
                "input",
                List.of(

                        Map.of(
                                "role",
                                "system",
                                "content",
                                SYSTEM_PROMPT
                        ),

                        Map.of(
                                "role",
                                "user",
                                "content",
                                buildUserPrompt(
                                        transcript,
                                        checklistItems
                                )
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
            String transcript,
            List<ChecklistItemInput> checklistItems
    ) {

        Map<String, Object> input =
                new LinkedHashMap<>();


        input.put(
                "transcript",
                transcript
        );


        input.put(
                "checklistItems",
                checklistItems
        );


        try {

            String json =
                    objectMapper.writeValueAsString(
                            input
                    );


            return """
                    다음 녹취문과 체크리스트를 분석하세요.

                    입력 데이터:
                    %s
                    """.formatted(
                    json
            );

        } catch (JsonProcessingException e) {

            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "체크리스트 분석 입력을 구성할 수 없습니다."
            );
        }
    }


    private Map<String, Object> buildResponseFormat() {

        /*
         * result item
         */

        Map<String, Object> resultProperties =
                new LinkedHashMap<>();


        resultProperties.put(
                "checklistItemId",
                Map.of(
                        "type",
                        "integer",
                        "description",
                        "입력으로 전달받은 체크리스트 ID"
                )
        );


        resultProperties.put(
                "status",
                Map.of(
                        "type",
                        "string",
                        "enum",
                        List.of(
                                "CHECKED",
                                "UNCHECKED",
                                "NEEDS_REVIEW"
                        )
                )
        );


        resultProperties.put(
                "confidenceScore",
                Map.of(
                        "type",
                        "number",
                        "minimum",
                        0,
                        "maximum",
                        1
                )
        );


        resultProperties.put(
                "evidenceText",
                Map.of(
                        "type",
                        "string",
                        "description",
                        "판단 근거가 되는 녹취문. 근거가 없으면 빈 문자열"
                )
        );


        resultProperties.put(
                "reason",
                Map.of(
                        "type",
                        "string",
                        "description",
                        "해당 상태로 판단한 이유"
                )
        );


        Map<String, Object> resultItem =
                new LinkedHashMap<>();


        resultItem.put(
                "type",
                "object"
        );


        resultItem.put(
                "properties",
                resultProperties
        );


        resultItem.put(
                "required",
                List.of(
                        "checklistItemId",
                        "status",
                        "confidenceScore",
                        "evidenceText",
                        "reason"
                )
        );


        resultItem.put(
                "additionalProperties",
                false
        );


        /*
         * results array
         */

        Map<String, Object> results =
                new LinkedHashMap<>();


        results.put(
                "type",
                "array"
        );

        results.put(
                "items",
                resultItem
        );


        /*
         * root
         */

        Map<String, Object> rootProperties =
                new LinkedHashMap<>();


        rootProperties.put(
                "summary",
                Map.of(
                        "type",
                        "string",
                        "description",
                        "전체 대화 분석을 요약한 짧은 한국어 문장"
                )
        );


        rootProperties.put(
                "results",
                results
        );


        Map<String, Object> schema =
                new LinkedHashMap<>();


        schema.put(
                "type",
                "object"
        );


        schema.put(
                "properties",
                rootProperties
        );


        schema.put(
                "required",
                List.of(
                        "summary",
                        "results"
                )
        );


        schema.put(
                "additionalProperties",
                false
        );


        Map<String, Object> format =
                new LinkedHashMap<>();


        format.put(
                "type",
                "json_schema"
        );


        format.put(
                "name",
                "checklist_analysis"
        );


        format.put(
                "strict",
                true
        );


        format.put(
                "schema",
                schema
        );


        return format;
    }


    private String extractOutputText(
            JsonNode responseBody
    ) {

        String status =
                responseBody
                        .path("status")
                        .asText();


        if ("incomplete".equals(
                status
        )) {

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OpenAI 응답 생성이 완료되지 않았습니다."
            );
        }


        JsonNode outputItems =
                responseBody.path(
                        "output"
                );


        if (!outputItems.isArray()) {

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OpenAI 응답에 output 배열이 없습니다."
            );
        }


        for (
                JsonNode outputItem
                : outputItems
        ) {

            if (!"message".equals(
                    outputItem
                            .path("type")
                            .asText()
            )) {

                continue;
            }


            JsonNode contents =
                    outputItem.path(
                            "content"
                    );


            if (!contents.isArray()) {

                continue;
            }


            for (
                    JsonNode content
                    : contents
            ) {

                String contentType =
                        content
                                .path("type")
                                .asText();


                if ("refusal".equals(
                        contentType
                )) {

                    throw new BusinessException(
                            ErrorCode.EXTERNAL_API_ERROR,
                            "OpenAI가 체크리스트 분석을 거부했습니다."
                    );
                }


                if ("output_text".equals(
                        contentType
                )) {

                    String text =
                            content
                                    .path("text")
                                    .asText();


                    if (!text.isBlank()) {

                        return text;
                    }
                }
            }
        }


        throw new BusinessException(
                ErrorCode.EXTERNAL_API_ERROR,
                "OpenAI 응답에서 체크리스트 분석 결과를 찾을 수 없습니다."
        );
    }
}