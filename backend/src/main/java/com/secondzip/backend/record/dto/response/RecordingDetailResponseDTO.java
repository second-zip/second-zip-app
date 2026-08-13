package com.secondzip.backend.record.dto.response;

import com.secondzip.backend.record.enums.RecordingStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecordingDetailResponseDTO {

    private Long recordingSessionId;

    private Long reportChecklistId;

    private String originalFileName;

    private String contentType;

    private Long fileSize;

    private RecordingStatus status;

    private String summary;
}