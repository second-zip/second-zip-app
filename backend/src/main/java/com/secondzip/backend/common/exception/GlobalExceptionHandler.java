package com.secondzip.backend.common.exception;

import com.secondzip.backend.common.dto.ErrorResponseDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 서비스에서 의도적으로 발생시킨 비즈니스 예외
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();

        ErrorResponseDTO response = ErrorResponseDTO.of(errorCode, e.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    /**
     * @Valid DTO 검증 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());

        ErrorResponseDTO response = ErrorResponseDTO.of(ErrorCode.INVALID_REQUEST, message, request.getRequestURI());

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    /**
     * 필수 Query Parameter 누락
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingParameter(
            MissingServletRequestParameterException e,
            HttpServletRequest request
    ) {
        String message = e.getParameterName() + " 값은 필수입니다.";

        ErrorResponseDTO response = ErrorResponseDTO.of(
                ErrorCode.MISSING_REQUIRED_VALUE,
                message,
                request.getRequestURI()
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    /**
     * 숫자·Enum 등 파라미터 타입 불일치
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        String message = e.getName() + " 값의 형식이 올바르지 않습니다.";

        ErrorResponseDTO response =
                ErrorResponseDTO.of(
                        ErrorCode.INVALID_PARAMETER,
                        message,
                        request.getRequestURI()
                );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    /**
     * 지원하지 않는 HTTP Method
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        ErrorResponseDTO response =
                ErrorResponseDTO.of(
                        ErrorCode.INVALID_REQUEST,
                        "지원하지 않는 HTTP 메서드입니다.",
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(response);
    }

    /**
     * 정의되지 않은 주소
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoHandlerFound(
            NoHandlerFoundException e,
            HttpServletRequest request
    ) {
        ErrorResponseDTO response =
                ErrorResponseDTO.of(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "요청한 API를 찾을 수 없습니다.",
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * 기존 IllegalArgumentException 임시 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(
            IllegalArgumentException e,
            HttpServletRequest request
    ) {
        ErrorResponseDTO response =
                ErrorResponseDTO.of(
                        ErrorCode.INVALID_REQUEST,
                        e.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    /**
     * 처리되지 않은 서버 오류
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("처리되지 않은 서버 오류: path={}", request.getRequestURI(), e);

        ErrorResponseDTO response =
                ErrorResponseDTO.of(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}