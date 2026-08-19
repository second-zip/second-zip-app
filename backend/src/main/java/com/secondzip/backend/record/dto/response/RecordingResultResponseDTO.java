package com.secondzip.backend.record.dto.response;

import com.secondzip.backend.record.dto.request.RecordingChecklistResultDTO;
import com.secondzip.backend.record.enums.RecordingStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RecordingResultResponseDTO {

    private Long recordingSessionId;

    private RecordingStatus status;

    private String summary;

    private List<RecordingChecklistResultDTO> results;
}