package com.secondzip.backend.record.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.record.dto.response.ClovaSpeechResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClovaSpeechClient implements SpeechToTextClient {

    private static final String OBJECT_STORAGE_PATH =
            "/recognizer/object-storage";

    private final ObjectMapper objectMapper;

    @Value("${CLOVA_SPEECH_INVOKE_URL}")
    private String invokeUrl;

    @Value("${CLOVA_SPEECH_SECRET_KEY}")
    private String secretKey;

    //objectKey == 음성파일
    @Override
    public String transcribe(String objectKey) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(
                "X-CLOVASPEECH-API-KEY",
                secretKey
        );

        Map<String, Object> requestBody =
                createRequestBody(objectKey);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(
                            buildRequestUrl(),
                            HttpMethod.POST,
                            request,
                            String.class
                    );

            return extractTranscript(response.getBody());

        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException(
                    "CLOVA Speech 요청에 실패했습니다. "
                            + "status="
                            + e.getStatusCode()
                            + ", response="
                            + e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "CLOVA Speech 처리 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    private Map<String, Object> createRequestBody(
            String objectKey
    ) {
        Map<String, Object> requestBody =
                new LinkedHashMap<>();

        requestBody.put("dataKey", objectKey);
        requestBody.put("language", "ko-KR");
        requestBody.put("completion", "sync");
        requestBody.put("wordAlignment", true);
        requestBody.put("fullText", true);

        return requestBody;
    }

    private String buildRequestUrl() {
        String normalizedUrl =
                invokeUrl.endsWith("/")
                        ? invokeUrl.substring(
                                0,
                                invokeUrl.length() - 1
                        )
                        : invokeUrl;

        if (normalizedUrl.endsWith(OBJECT_STORAGE_PATH)) {
            return normalizedUrl;
        }

        return normalizedUrl + OBJECT_STORAGE_PATH;
    }

    private String extractTranscript(
            String responseBody
    ) throws JsonProcessingException {

        if (responseBody == null
                || responseBody.isBlank()) {
            throw new IllegalStateException(
                    "CLOVA Speech 응답이 비어 있습니다."
            );
        }

        ClovaSpeechResponse response =
                objectMapper.readValue(
                        responseBody,
                        ClovaSpeechResponse.class
                );

        if (response.getText() != null
                && !response.getText().isBlank()) {
            return response.getText().trim();
        }

        if (response.getSegments() != null
                && !response.getSegments().isEmpty()) {

            String transcript =
                    response.getSegments()
                            .stream()
                            .map(
                                    ClovaSpeechResponse
                                            .Segment::getText
                            )
                            .filter(text ->
                                    text != null
                                            && !text.isBlank()
                            )
                            .collect(
                                    Collectors.joining(" ")
                            );

            if (!transcript.isBlank()) {
                return transcript.trim();
            }
        }

        throw new IllegalStateException(
                "CLOVA Speech 응답에서 녹취문을 찾을 수 없습니다. "
                        + "result="
                        + response.getResult()
                        + ", message="
                        + response.getMessage()
        );
    }
}