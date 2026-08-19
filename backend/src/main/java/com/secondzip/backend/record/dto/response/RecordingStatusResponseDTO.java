package com.secondzip.backend.record.dto.response;

import com.secondzip.backend.record.enums.RecordingStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecordingStatusResponseDTO {

    private Long recordingSessionId;

    private RecordingStatus status;

    private String transcript;

    private String summary;

    private String failureReason;
}