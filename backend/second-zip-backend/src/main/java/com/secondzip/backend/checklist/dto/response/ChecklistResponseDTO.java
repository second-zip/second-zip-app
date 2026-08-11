package com.secondzip.backend.checklist.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChecklistResponseDTO {

    private Long checklistItemId;

    private String contents;

    private String category;

    private Boolean checked;
}