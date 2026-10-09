package com.zerozoa.psik.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("커스텀 메시지가 있으면 응답 message에 그대로 담긴다")
    void businessException_customMessage() {
        // given
        BusinessException e = new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "허용되지 않는 파일 형식입니다: .pdf");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(e);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getCode()).isEqualTo("C001");
        assertThat(response.getBody().getMessage()).isEqualTo("허용되지 않는 파일 형식입니다: .pdf");
    }

    @Test
    @DisplayName("커스텀 메시지가 없으면 ErrorCode 기본 메시지가 담긴다")
    void businessException_defaultMessage() {
        // given
        BusinessException e = new BusinessException(ErrorCode.POST_NOT_FOUND);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(e);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.POST_NOT_FOUND.getMessage());
    }
}
