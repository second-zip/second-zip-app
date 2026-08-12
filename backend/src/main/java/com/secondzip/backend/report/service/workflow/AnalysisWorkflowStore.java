package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.report.dto.AnalysisWorkflowState;

public interface AnalysisWorkflowStore {
    void save(AnalysisWorkflowState state);

    AnalysisWorkflowState findOwned(String requestId, Long accountId);

    void delete(String requestId);

    String tryAcquireExecutionLock(String requestId);

    void releaseExecutionLock(String requestId, String lockToken);
}
