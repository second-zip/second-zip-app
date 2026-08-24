package com.secondzip.backend.record.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecordingFileUrlResponseDTO {

    private String url;

    private String originalFileName;

    private String contentType;

    private Long fileSize;

    // 초 단위
    private Long expiresIn;
}