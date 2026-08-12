package com.secondzip.backend.record.mapper;

import com.secondzip.backend.record.dto.request.RecordingChecklistResultDTO;
import com.secondzip.backend.record.dto.response.ChecklistAnalysisResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChecklistAnalysisResultMapper {

    int upsertResult(
            @Param("recordingSessionId") Long recordingSessionId,
            @Param("item") ChecklistAnalysisResult.ResultItem item
    );

    int deleteByRecordingSessionId(
            @Param("recordingSessionId") Long recordingSessionId
    );
    List<RecordingChecklistResultDTO> findByRecordingSessionId(
            @Param("recordingSessionId") Long recordingSessionId
    );
}