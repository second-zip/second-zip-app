package com.secondzip.backend.record.mapper;

import com.secondzip.backend.record.domain.RecordingSessionVO;
import com.secondzip.backend.record.enums.RecordingStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecordingSessionMapper {

    int insert(RecordingSessionVO session);

    RecordingSessionVO findByIdAndAccountId(
            @Param("recordingSessionId") Long recordingSessionId,
            @Param("accountId") Long accountId
    );

    int updateStatus(
            @Param("recordingSessionId") Long recordingSessionId,
            @Param("status") RecordingStatus status
    );

    int updateTranscript(
            @Param("recordingSessionId") Long recordingSessionId,
            @Param("fullTranscript") String fullTranscript,
            @Param("status") RecordingStatus status
    );

    int markFailed(
            @Param("recordingSessionId") Long recordingSessionId,
            @Param("failureReason") String failureReason
    );
}