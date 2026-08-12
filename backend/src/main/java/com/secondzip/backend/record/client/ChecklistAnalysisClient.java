package com.secondzip.backend.record.client;

import com.secondzip.backend.record.dto.request.ChecklistItemInput;
import com.secondzip.backend.record.dto.response.ChecklistAnalysisResult;

import java.util.List;

public interface ChecklistAnalysisClient {

    ChecklistAnalysisResult analyze(String transcript, List<ChecklistItemInput> checklistItems);
}