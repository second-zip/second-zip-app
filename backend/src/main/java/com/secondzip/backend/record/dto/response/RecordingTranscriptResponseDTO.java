package com.secondzip.backend.record.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecordingTranscriptResponseDTO {

    private Long recordingSessionId;

    private String transcript;
}