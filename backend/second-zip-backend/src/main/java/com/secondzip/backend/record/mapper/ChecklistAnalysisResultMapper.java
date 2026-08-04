package com.secondzip.backend.record.mapper;

import com.secondzip.backend.record.dto.response.ChecklistAnalysisResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChecklistAnalysisResultMapper {

    int upsertResult(
            @Param("recordingSessionId") Long recordingSessionId,
            @Param("item") ChecklistAnalysisResult.ResultItem item
    );

    int deleteByRecordingSessionId(
            @Param("recordingSessionId") Long recordingSessionId
    );
}