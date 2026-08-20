package com.secondzip.backend.report.dto;

import com.secondzip.backend.checklist.enums.Category;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * 분석 판정이 "확인했고 안전함"(VERIFIED + SAFE)으로 나온 체크리스트 항목.
 *
 * 확인은 했지만 CAUTION/DANGER 로 판정된 항목은 여기 담기지 않음.
 * 판단 기준은 ChecklistAutoCheckResolver 참고.
 *
 * checklist_items 의 PK는 환경마다 값이 달라질 수 있으므로,
 * UNIQUE (category, contents) 를 자연키로 삼아 항목을 지목.
 *
 * MyBatis 파라미터로 그대로 넘어가므로 record 가 아닌
 * JavaBean 게터를 가진 클래스로 둠. (MyBatis 3.5 의 Reflector 는
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
