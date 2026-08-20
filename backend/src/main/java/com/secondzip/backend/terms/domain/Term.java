package com.secondzip.backend.terms.domain;

import com.secondzip.backend.terms.enums.TermType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Term {
    private Long termId;
    private String title;
    private String content;
    private TermType termType;
    private Boolean required;
    private String version;
}
