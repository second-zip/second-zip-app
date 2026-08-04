package com.secondzip.backend.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SpecialTermView {
    private Integer sequence;
    private String title;
    private String content;
}
