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

    RecordingSessionVO findById(
            @Param("recordingSessionId")
            Long recordingSessionId
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

    int updateSummary(
            @Param("recordingSessionId")
            Long recordingSessionId,

            @Param("summary")
            String summary
    );

    int markFailed(
            @Param("recordingSessionId") Long recordingSessionId,
            @Param("failureReason") String failureReason
    );

    int insertLiveSession(
            RecordingSessionVO session
    );

    int deleteByIdAndAccountId(
            @Param("recordingSessionId")
            Long recordingSessionId,

            @Param("accountId")
            Long accountId
    );

    int deleteAnalysisResults(
            @Param("recordingSessionId")
            Long recordingSessionId
    );
}