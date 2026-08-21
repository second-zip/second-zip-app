package com.secondzip.backend.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ai_generate_messages 테이블 한 행. (AI 추천 특약)
 *
 * 사용자에게 보이는 순번(sequence)은 이 테이블에 없음.
 * 조회 순서대로 서비스가 붙임.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSpecialTerm {

    private Long aiGenerateMessageId;
    private Long analysisReportId;
    private String title;
    private String content;
}
