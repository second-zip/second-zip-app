package com.secondzip.backend.record.domain;

import com.secondzip.backend.record.enums.RecordingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingSessionVO {

    private Long recordingSessionId;

    private Long accountId;

    private Long reportChecklistId;

    private String originalFileName;

    private String storageObjectKey;

    private String contentType;

    private Long fileSize;

    private RecordingStatus status;

    private String fullTranscript;

    private String summary;

    private String failureReason;
}