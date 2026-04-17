package com.zerozoa.skinner.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //비즈니스 예외 처리
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        if (e.getErrorCode().getStatus().is5xxServerError()) {
            log.error("[Business Exception] Code: {}, Message: {}", e.getErrorCode().getCode(), e.getMessage());
        } else {
            log.warn("[Business Exception] Code: {}, Message: {}", e.getErrorCode().getCode(), e.getMessage());
        }
        return ErrorResponse.toResponseEntity(e.getErrorCode());
    }

    //@Valid 유효성 검사 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("잘못된 입력값입니다.");
        log.warn("[Validation Exception] {}", message);
        return ErrorResponse.toResponseEntity(ErrorCode.INVALID_INPUT_VALUE, message);
    }

    //그 외 모든 알 수 없는 서버 에러 처리
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[Internal Server Error] ", e);
        return ErrorResponse.toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}