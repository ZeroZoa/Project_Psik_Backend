package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.dto.auth.TokenResponse;
import com.zerozoa.skinner.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용하여 Access Token을 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @RequestHeader("Refresh-Token") String refreshToken, // 헤더로 받음
            HttpServletRequest request
    ) {
        // 접속 정보 추출
        String ip = getIp(request);
        String userAgent = request.getHeader("User-Agent");

        TokenResponse tokenResponse = authService.reissue(refreshToken, ip, userAgent);
        return ResponseEntity.ok(tokenResponse);
    }

    // IP 추출 유틸 (공통화해서 Utils 클래스로 빼는 것도 좋습니다)
    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        return ip;
    }
}
