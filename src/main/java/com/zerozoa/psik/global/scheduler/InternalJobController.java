package com.zerozoa.psik.global.scheduler;

import com.zerozoa.psik.global.security.GcpOidcVerifier;
import com.zerozoa.psik.service.diary.SkinAnalysisSweeper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cloud Scheduler가 호출하는 주기 작업.
 * Cloud Run이 요청 처리 중에만 CPU를 할당하므로 @Scheduled 대신 외부에서 요청으로 깨운다.
 */
@Tag(name = "Internal API", description = "Cloud Scheduler 주기 작업 (Google OIDC 인증)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/jobs")
public class InternalJobController {

    private final GcpOidcVerifier oidcVerifier;
    private final SkinAnalysisSweeper skinAnalysisSweeper;
    private final TokenCleanupScheduler tokenCleanupScheduler;

    @Operation(summary = "오래된 PENDING 분석 정리 (5분마다)")
    @PostMapping("/sweep-pending")
    public ResponseEntity<Void> sweepPending(@RequestHeader(value = "Authorization", required = false) String authorization) {
        oidcVerifier.verify(authorization);
        skinAnalysisSweeper.sweepStalePending();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "만료 Refresh Token 정리 (매일 03시 UTC)")
    @PostMapping("/cleanup-tokens")
    public ResponseEntity<Void> cleanupTokens(@RequestHeader(value = "Authorization", required = false) String authorization) {
        oidcVerifier.verify(authorization);
        tokenCleanupScheduler.cleanupExpiredTokens();
        return ResponseEntity.noContent().build();
    }
}
