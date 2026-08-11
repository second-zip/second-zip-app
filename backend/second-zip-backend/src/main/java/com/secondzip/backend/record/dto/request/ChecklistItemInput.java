package com.secondzip.backend.record.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChecklistItemInput {

    private Long checklistItemId;
    private String contents;
    private String category;
}