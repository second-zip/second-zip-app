package com.secondzip.backend.record.dto.request;

import com.secondzip.backend.checklist.enums.Category;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChecklistItemInput {

    private Long checklistItemId;
    private String contents;
    private Category category;
}