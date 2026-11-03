package com.zerozoa.psik.controller.diary;

import com.zerozoa.psik.dto.diary.PubSubPushRequest;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.global.security.GcpOidcVerifier;
import com.zerozoa.psik.service.diary.SkinAnalysisWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkinAnalysisPushControllerTest {

    private static final String AUTH = "Bearer token";
    private static final String PAYLOAD = "{\"skinAnalysisId\":1}";

    @Mock GcpOidcVerifier oidcVerifier;
    @Mock SkinAnalysisWorker worker;
    @InjectMocks SkinAnalysisPushController controller;

    @Test
    @DisplayName("base64 본문을 풀어 워커에 넘기고, 처리 완료면 204(ack)로 응답한다")
    void handled_204() {
        // given
        when(worker.handle(PAYLOAD)).thenReturn(true);

        // when
        ResponseEntity<Void> response = controller.receive(AUTH, push(PAYLOAD));

        // then
        verify(oidcVerifier).verify(AUTH);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("일시 실패면 503으로 응답해 Pub/Sub가 재전달하게 한다")
    void retry_503() {
        // given
        when(worker.handle(PAYLOAD)).thenReturn(false);

        // when
        ResponseEntity<Void> response = controller.receive(AUTH, push(PAYLOAD));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("OIDC 검증에 실패하면 워커를 호출하지 않는다")
    void invalidToken_noProcessing() {
        // given
        doThrow(new BusinessException(ErrorCode.INVALID_TOKEN)).when(oidcVerifier).verify(anyString());

        // when & then
        assertThatThrownBy(() -> controller.receive(AUTH, push(PAYLOAD))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(worker);
    }

    private PubSubPushRequest push(String json) {
        String data = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return new PubSubPushRequest(new PubSubPushRequest.Message(data, "m-1"));
    }
}
