package com.secondzip.backend.record.dto.response;

import com.secondzip.backend.record.enums.RecordingStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecordingLiveStartResponseDTO {

    private Long recordingSessionId;
    private RecordingStatus status;
}