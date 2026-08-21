package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.report.dto.AnalysisWorkflowStateDTO;

public interface AnalysisWorkflowStore {
    void save(AnalysisWorkflowStateDTO state);

    AnalysisWorkflowStateDTO findOwned(String requestId, Long accountId);

    void delete(String requestId);

    String tryAcquireExecutionLock(String requestId);

    void releaseExecutionLock(String requestId, String lockToken);
}
