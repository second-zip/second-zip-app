package com.secondzip.backend.checklist.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
public class ChecklistCheckRequestDTO {

    @NotNull
    private Boolean checked;
}