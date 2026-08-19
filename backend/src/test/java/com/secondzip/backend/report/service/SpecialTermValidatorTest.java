package com.secondzip.backend.report.service;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.SpecialTermResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpecialTermValidatorTest {

    private final SpecialTermValidator validator =
            new SpecialTermValidator();

    @Test
    @DisplayName("정상적인 특약 3개는 검증을 통과하고 공백이 제거된다")
    void validatesAndNormalizesValidTerms() {
        List<SpecialTermResult> terms = List.of(
                new SpecialTermResult(
                        "  추가 담보권 설정 금지  ",
                        "  임대인은 계약 후 새로운 담보권을 설정하지 않는다.  "
                ),
                new SpecialTermResult(
                        "잔금 전 등기사항 재확인",
                        "임대인은 잔금 지급 전 최신 등기사항을 제공하여야 한다."
                ),
                new SpecialTermResult(
                        "보증보험 가입 조건",
                        "보증보험 가입이 불가능한 경우 임차인은 계약을 해제할 수 있다."
                )
        );

        List<SpecialTermResult> result =
                validator.validateAndNormalize(terms);

        assertEquals(3, result.size());

        assertEquals(
                "추가 담보권 설정 금지",
                result.get(0).getTitle()
        );

        assertEquals(
                "임대인은 계약 후 새로운 담보권을 설정하지 않는다.",
                result.get(0).getContent()
        );
    }

    @Test
    @DisplayName("특약이 3개 미만이면 예외가 발생한다")
    void rejectsTooFewTerms() {
        List<SpecialTermResult> terms = List.of(
                new SpecialTermResult("특약1", "내용1"),
                new SpecialTermResult("특약2", "내용2")
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validateAndNormalize(terms)
        );

        assertEquals(
                ErrorCode.EXTERNAL_API_ERROR,
                exception.getErrorCode()
        );

        assertEquals(
                "AI 특약은 3개 이상 5개 이하이어야 합니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("특약이 5개를 초과하면 예외가 발생한다")
    void rejectsTooManyTerms() {
        List<SpecialTermResult> terms = List.of(
                new SpecialTermResult("특약1", "내용1"),
                new SpecialTermResult("특약2", "내용2"),
                new SpecialTermResult("특약3", "내용3"),
                new SpecialTermResult("특약4", "내용4"),
                new SpecialTermResult("특약5", "내용5"),
                new SpecialTermResult("특약6", "내용6")
        );

        assertThrows(
                BusinessException.class,
                () -> validator.validateAndNormalize(terms)
        );
    }

    @Test
    @DisplayName("제목이 비어 있으면 예외가 발생한다")
    void rejectsBlankTitle() {
        List<SpecialTermResult> terms = List.of(
                new SpecialTermResult("   ", "내용1"),
                new SpecialTermResult("특약2", "내용2"),
                new SpecialTermResult("특약3", "내용3")
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validateAndNormalize(terms)
        );

        assertEquals(
                "AI 특약 제목이 비어 있습니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("본문이 비어 있으면 예외가 발생한다")
    void rejectsBlankContent() {
        List<SpecialTermResult> terms = List.of(
                new SpecialTermResult("특약1", " "),
                new SpecialTermResult("특약2", "내용2"),
                new SpecialTermResult("특약3", "내용3")
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validateAndNormalize(terms)
        );

        assertEquals(
                "AI 특약 본문이 비어 있습니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("50자를 초과한 제목은 거부한다")
    void rejectsTooLongTitle() {
        List<SpecialTermResult> terms = List.of(
                new SpecialTermResult("가".repeat(51), "내용1"),
                new SpecialTermResult("특약2", "내용2"),
                new SpecialTermResult("특약3", "내용3")
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validateAndNormalize(terms)
        );

        assertEquals(
                "AI 특약 제목은 50자 이하여야 합니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("200자를 초과한 본문은 거부한다")
    void rejectsTooLongContent() {
        List<SpecialTermResult> terms = List.of(
                new SpecialTermResult("특약1", "가".repeat(201)),
                new SpecialTermResult("특약2", "내용2"),
                new SpecialTermResult("특약3", "내용3")
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validateAndNormalize(terms)
        );

        assertEquals(
                "AI 특약 본문은 200자 이하여야 합니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("중복된 특약 제목은 거부한다")
    void rejectsDuplicateTitles() {
        List<SpecialTermResult> terms = List.of(
                new SpecialTermResult(
                        "추가 담보권 설정 금지",
                        "내용1"
                ),
                new SpecialTermResult(
                        "추가 담보권 설정 금지",
                        "내용2"
                ),
                new SpecialTermResult(
                        "보증보험 가입 조건",
                        "내용3"
                )
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validateAndNormalize(terms)
        );

        assertEquals(
                "중복된 특약 제목이 생성되었습니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("중복된 특약 본문은 거부한다")
    void rejectsDuplicateContents() {
        List<SpecialTermResult> terms = List.of(
                new SpecialTermResult(
                        "특약1",
                        "임대인은 새로운 담보권을 설정하지 않는다."
                ),
                new SpecialTermResult(
                        "특약2",
                        "임대인은 새로운 담보권을 설정하지 않는다."
                ),
                new SpecialTermResult(
                        "특약3",
                        "임대인은 잔금 전 등기사항을 제공한다."
                )
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validateAndNormalize(terms)
        );

        assertEquals(
                "중복된 특약 내용이 생성되었습니다.",
                exception.getMessage()
        );
    }
}