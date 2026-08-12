package com.secondzip.backend.checklist.dto.response;

import com.secondzip.backend.checklist.enums.Category;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChecklistResponseDTO {

    private Long checklistItemId;

    private String contents;

    private Category category;

    private String description;

    private Boolean checked;
}