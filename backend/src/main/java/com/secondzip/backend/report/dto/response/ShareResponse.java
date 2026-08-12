package com.secondzip.backend.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ShareResponse {
    private String shareToken;
    private LocalDateTime shareExpiresAt;
}