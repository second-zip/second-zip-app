package com.secondzip.backend.report.service;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.SpecialTermResultDTO;
import com.secondzip.backend.report.dto.response.SpecialTermView;
import com.secondzip.backend.report.mapper.StubReportMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpecialTermPersistenceServiceTest {

    @Test
    void replacesExistingTermsAfterLockingReport() {
        CapturingMapper mapper = new CapturingMapper(1L);
        SpecialTermPersistenceService service =
                new SpecialTermPersistenceService(mapper);

        List<SpecialTermResultDTO> terms = List.of(
                new SpecialTermResultDTO(
                        "추가 담보권 설정 금지",
                        "임대인은 잔금 지급 전까지 새로운 담보권을 설정하지 않는다."
                ),
                new SpecialTermResultDTO(
                        "잔금 전 등기사항 재확인",
                        "임대인은 잔금 지급 전 최신 등기사항을 제공하여야 한다."
                ),
                new SpecialTermResultDTO(
                        "보증보험 가입 조건",
                        "보증보험 가입이 불가능한 경우 임차인은 계약을 해제할 수 있다."
                )
        );

        List<SpecialTermView> result =
                service.replace(1L, terms);

        // 리포트 잠금 → 기존 특약 삭제 → 신규 특약 저장 순서 확인
        assertEquals(
                List.of(
                        "lock",
                        "delete",
                        "insert",
                        "insert",
                        "insert"
                ),
                mapper.events
        );

        assertEquals(3, mapper.insertedTerms.size());

        assertEquals(
                "추가 담보권 설정 금지",
                mapper.insertedTerms.get(0).getTitle()
        );

        // 응답 sequence가 1부터 생성되는지 확인
        assertEquals(1, result.get(0).getSequence());
        assertEquals(2, result.get(1).getSequence());
        assertEquals(3, result.get(2).getSequence());
    }

    @Test
    void doesNotDeleteExistingTermsWhenReportDoesNotExist() {
        CapturingMapper mapper = new CapturingMapper(null);
        SpecialTermPersistenceService service =
                new SpecialTermPersistenceService(mapper);

        List<SpecialTermResultDTO> terms = List.of(
                new SpecialTermResultDTO("특약1", "내용1"),
                new SpecialTermResultDTO("특약2", "내용2"),
                new SpecialTermResultDTO("특약3", "내용3")
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.replace(999L, terms)
        );

        assertEquals(
                ErrorCode.RESOURCE_NOT_FOUND,
                exception.getErrorCode()
        );

        assertEquals(
                "리포트를 찾을 수 없습니다.",
                exception.getMessage()
        );

        // lock에서 실패했으므로 기존 데이터 삭제가 일어나면 안 됨
        assertEquals(
                List.of("lock"),
                mapper.events
        );

        assertEquals(0, mapper.insertedTerms.size());
    }

    private static class CapturingMapper extends StubReportMapper {

        private final Long lockResult;

        private final List<String> events =
                new ArrayList<>();

        private final List<SpecialTermResultDTO> insertedTerms =
                new ArrayList<>();

        private CapturingMapper(Long lockResult) {
            this.lockResult = lockResult;
        }

        @Override
        public Long lockReportById(Long reportId) {
            events.add("lock");
            return lockResult;
        }

        @Override
        public void deleteSpecialTermsByReportId(Long reportId) {
            events.add("delete");
        }

        @Override
        public void insertSpecialTerm(
                Long reportId,
                String title,
                String content
        ) {
            events.add("insert");

            insertedTerms.add(
                    new SpecialTermResultDTO(title, content)
            );
        }
    }
}