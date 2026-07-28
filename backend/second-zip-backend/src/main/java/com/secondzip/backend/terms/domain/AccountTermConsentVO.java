package com.secondzip.backend.terms.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTermConsentVO {

    private Long accountTermConsentId;
    private Long accountId;
    private Long termId;
    private Boolean agreed;
    private LocalDateTime agreedAt;
}