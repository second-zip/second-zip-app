package com.secondzip.backend.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodefTwoWayStateDTO {
    private Integer jobIndex;
    private Integer threadIndex;
    private String jti;
    private String twoWayTimestamp;
}
