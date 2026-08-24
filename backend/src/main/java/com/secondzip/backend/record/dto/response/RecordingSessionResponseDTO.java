package com.secondzip.backend.record.dto.response;

import com.secondzip.backend.record.domain.RecordingSession;
import com.secondzip.backend.record.enums.RecordingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingSessionResponseDTO {

    private Long recordingSessionId;
    private RecordingStatus status;

    public static RecordingSessionResponseDTO from(
            RecordingSession session
    ) {
        return RecordingSessionResponseDTO.builder()
                .recordingSessionId(
                        session.getRecordingSessionId()
                )
                .status(session.getStatus())
                .build();
    }
}