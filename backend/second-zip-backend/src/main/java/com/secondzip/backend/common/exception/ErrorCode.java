package com.secondzip.backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통 요청 오류
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "요청값이 올바르지 않습니다."),
    MISSING_REQUIRED_VALUE(HttpStatus.BAD_REQUEST, "COMMON_400", "필수값이 누락되었습니다."),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON_400", "요청 파라미터가 올바르지 않습니다."),
    INVALID_ENUM_VALUE(HttpStatus.BAD_REQUEST, "COMMON_400", "유효하지 않은 값입니다."),
    // 리소스
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청한 데이터를 찾을 수 없습니다."),
    // 중복·충돌
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "COMMON_409", "이미 존재하는 데이터입니다."),
    RESOURCE_CONFLICT(HttpStatus.CONFLICT, "COMMON_409", "현재 상태에서는 요청을 처리할 수 없습니다."),
    // 서버 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "COMMON_502", "외부 API 연동 중 오류가 발생했습니다."
    ),

    // 인증·인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "만료된 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_004", "접근 권한이 없습니다."),

    //약관 동의 여부
    REQUIRED_TERM_NOT_AGREED(HttpStatus.BAD_REQUEST, "TERM_001", "필수 약관에 동의해야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}