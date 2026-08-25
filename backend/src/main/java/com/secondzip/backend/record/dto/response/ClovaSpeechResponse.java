package com.secondzip.backend.record.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClovaSpeechResponse {

    private String result;
    private String message;
    private String text;
    private List<Segment> segments;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Segment {

        private String text;

        // CLOVA 화자 분리 결과
        private Diarization diarization;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Diarization {

        // "1", "2", ...
        private String label;
    }
}