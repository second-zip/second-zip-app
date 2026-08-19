package com.secondzip.backend.report.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReportServiceTest {

    @Test
    @DisplayName("리포트 변경 명령을 영속성 서비스에 그대로 위임한다")
    void delegatesReportMutations() {
        ReportPersistenceService persistenceService =
                mock(ReportPersistenceService.class);
        ReportService service = new ReportService(persistenceService);

        service.deleteReport(7L, 77L);
        service.addFavorite(7L, 77L);
        service.removeFavorite(7L, 77L);

        verify(persistenceService).deleteReport(7L, 77L);
        verify(persistenceService).addFavorite(7L, 77L);
        verify(persistenceService).removeFavorite(7L, 77L);
    }
}
