package com.secondzip.backend.report.dto.request;

import lombok.Data;

@Data
public class CreateReportRequest {
    private String roadAddress;
    private String detailAddress;
    private Long deposit;
}