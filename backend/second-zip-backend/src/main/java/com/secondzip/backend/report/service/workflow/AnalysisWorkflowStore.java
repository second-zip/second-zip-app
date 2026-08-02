package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.report.dto.AnalysisWorkflowState;

public interface AnalysisWorkflowStore {
    void save(AnalysisWorkflowState state);

    AnalysisWorkflowState findOwned(String requestId, Long accountId);

    void delete(String requestId);

    default boolean tryAcquireExecutionLock(String requestId) {
        return true;
    }

    default void releaseExecutionLock(String requestId) {
    }
}
