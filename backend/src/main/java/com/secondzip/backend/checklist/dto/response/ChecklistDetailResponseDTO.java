package com.secondzip.backend.checklist.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChecklistDetailResponseDTO {

    private String roadAddress;

    private String detailAddress;

    private List<ChecklistResponseDTO> items;
}