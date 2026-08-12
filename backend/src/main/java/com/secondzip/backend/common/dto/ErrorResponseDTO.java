package com.secondzip.backend.common.dto;

import com.secondzip.backend.common.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponseDTO {

    private int status;
    private String code;
    private String message;
    private String path;
    private LocalDateTime timestamp;

    public static ErrorResponseDTO of(
            ErrorCode errorCode,
            String path
    ) {
        return ErrorResponseDTO.builder()
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponseDTO of(
            ErrorCode errorCode,
            String message,
            String path
    ) {
        return ErrorResponseDTO.builder()
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}