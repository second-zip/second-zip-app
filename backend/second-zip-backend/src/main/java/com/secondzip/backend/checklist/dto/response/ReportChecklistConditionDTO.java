package com.secondzip.backend.checklist.dto.response;

import com.secondzip.backend.checklist.enums.Category;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportChecklistConditionDTO {

    private Category housingCategory;

    private Boolean trustProperty;
}