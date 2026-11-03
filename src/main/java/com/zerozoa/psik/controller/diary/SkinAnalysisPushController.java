package com.zerozoa.psik.controller.diary;

import com.zerozoa.psik.dto.diary.PubSubPushRequest;
import com.zerozoa.psik.global.security.GcpOidcVerifier;
import com.zerozoa.psik.service.diary.SkinAnalysisWorker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Tag(name = "Internal API", description = "Pub/Sub push 수신 (Google OIDC 인증)")
@RestController
@RequiredArgsConstructor
public class SkinAnalysisPushController {

    private final GcpOidcVerifier oidcVerifier;
    private final SkinAnalysisWorker worker;

    // 2xx 응답 = ack, 그 외 = Pub/Sub가 간격을 두고 재전달
    @Operation(summary = "피부 분석 요청 메시지 수신")
    @PostMapping("/internal/pubsub/skin-analysis")
    public ResponseEntity<Void> receive(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @Valid @RequestBody PubSubPushRequest request) {
        oidcVerifier.verify(authorization);
        String payload = new String(Base64.getDecoder().decode(request.message().data()), StandardCharsets.UTF_8);
        return worker.handle(payload)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}
