package com.secondzip.backend.report.service;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.SpecialTermResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SpecialTermValidator {

    private static final int MIN_TERM_COUNT = 3;
    private static final int MAX_TERM_COUNT = 5;
    private static final int MAX_TITLE_LENGTH = 50;
    private static final int MAX_CONTENT_LENGTH = 200;

    public List<SpecialTermResult> validateAndNormalize(List<SpecialTermResult> terms) {
        if (terms == null || terms.size() < MIN_TERM_COUNT || terms.size() > MAX_TERM_COUNT) {
            throw invalidResponse(
                    "AI 특약은 3개 이상 5개 이하이어야 합니다."
            );
        }

        List<SpecialTermResult> normalizedTerms = new ArrayList<>();

        Set<String> titles = new HashSet<>();
        Set<String> contents = new HashSet<>();

        for (SpecialTermResult term : terms) {
            if (term == null) {
                throw invalidResponse(
                        "AI 특약 응답에 비어 있는 항목이 있습니다."
                );
            }

            String title = normalize(term.getTitle());
            String content = normalize(term.getContent());

            validateTitle(title);
            validateContent(content);

            if (!titles.add(title)) {
                throw invalidResponse(
                        "중복된 특약 제목이 생성되었습니다."
                );
            }

            if (!contents.add(content)) {
                throw invalidResponse(
                        "중복된 특약 내용이 생성되었습니다."
                );
            }

            normalizedTerms.add(new SpecialTermResult(title, content));
        }

        return normalizedTerms;
    }

    private void validateTitle(String title) {
        if (title.isBlank()) {
            throw invalidResponse(
                    "AI 특약 제목이 비어 있습니다."
            );
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            throw invalidResponse("AI 특약 제목은 50자 이하여야 합니다.");
        }
    }

    private void validateContent(String content) {
        if (content.isBlank()) {
            throw invalidResponse("AI 특약 본문이 비어 있습니다.");
        }

        if (content.length() > MAX_CONTENT_LENGTH) {
            throw invalidResponse("AI 특약 본문은 200자 이하여야 합니다.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private BusinessException invalidResponse(String message) {
        return new BusinessException(ErrorCode.EXTERNAL_API_ERROR, message);
    }
}
