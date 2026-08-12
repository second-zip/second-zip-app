package com.secondzip.backend.record.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClovaRealtimeResponse {

    private String uid;

    private List<String> responseType;

    private Transcription transcription;


    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Transcription {

        private String text;

        private Integer position; //전체 transcript에서 이 텍스트가 시작되는 위치

        private Boolean epFlag; //음성 전송 중인지 아닌지

        private Integer seqId;

        private Double confidence; //음성 인식 결과의 신뢰도
    }
}