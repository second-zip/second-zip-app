package com.secondzip.backend.report.dto;

import com.secondzip.backend.checklist.enums.Category;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * 분석 판정으로 "확인 완료" 처리된 체크리스트 항목.
 *
 * checklist_items 의 PK는 환경마다 값이 달라질 수 있으므로,
 * UNIQUE (category, contents) 를 자연키로 삼아 항목을 지목한다.
 *
 * MyBatis 파라미터로 그대로 넘어가므로 record 가 아닌
 * JavaBean 게터를 가진 클래스로 둔다. (MyBatis 3.5 의 Reflector 는
 * record 의 접근자 메서드를 게터로 인식하지 못한다.)
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public class VerifiedChecklistItem {

    private final Category category;
    private final String contents;
}
