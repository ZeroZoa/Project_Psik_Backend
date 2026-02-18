package com.zerozoa.skinner.global.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

@Getter
@Builder
public class ErrorResponse {
    @Builder.Default
    private final Instant timestamp = Instant.now();

    private final int status;
    private final String error;
    private final String code;
    private final String message;

    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getStatus().value())
                        .error(errorCode.getStatus().name())
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build()
                );
    }
}