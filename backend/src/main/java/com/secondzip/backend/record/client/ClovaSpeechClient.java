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
import java.util.List;
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

    //objectKey == 음성 파일의 주소 이름
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

        } catch (HttpStatusCodeException e) { //서버 측 오류
            throw new IllegalStateException(
                    "CLOVA Speech 요청에 실패했습니다. "
                            + "status="
                            + e.getStatusCode()
                            + ", response="
                            + e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) { //일반 예외
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

        Map<String, Object> diarization = new LinkedHashMap<>();

        diarization.put("enable", true);

        requestBody.put(
                "diarization",
                diarization
        );

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

        // 화자 정보가 포함된 segments 우선 사용
        if (response.getSegments() != null
                && !response.getSegments().isEmpty()) {

            String transcript =
                    buildSpeakerTranscript(
                            response.getSegments()
                    );

            if (!transcript.isBlank()) {
                return transcript.trim();
            }
        }

        // segments가 없는 경우에만 fullText 사용
        if (response.getText() != null
                && !response.getText().isBlank()) {
            return response.getText().trim();
        }


        throw new IllegalStateException(
                "CLOVA Speech 응답에서 녹취문을 찾을 수 없습니다. "
                        + "result="
                        + response.getResult()
                        + ", message="
                        + response.getMessage()
        );
    }

    private String buildSpeakerTranscript(List<ClovaSpeechResponse.Segment> segments) {

        StringBuilder transcript = new StringBuilder();

        String previousSpeaker = null;

        for (ClovaSpeechResponse.Segment segment : segments) {

            String text = segment.getText();

            if (text == null || text.isBlank()) {
                continue;
            }

            String speaker = null;

            if (segment.getDiarization() != null) {
                speaker = segment
                        .getDiarization()
                        .getLabel();
            }

            if (speaker == null || speaker.isBlank()) {
                speaker = "알 수 없음";
            }


            // 같은 화자가 계속 말한 경우 한 줄로 연결
            if (speaker.equals(previousSpeaker)) {

                transcript
                        .append(" ")
                        .append(text.trim());

            } else {

                if (transcript.length() > 0) {
                    transcript.append("\n");
                }

                transcript
                        .append("화자 ")
                        .append(speaker)
                        .append(": ")
                        .append(text.trim());
            }

            previousSpeaker = speaker;
        }

        return transcript.toString().trim();
    }
}