package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.dto.auth.TokenResponse;
import com.zerozoa.skinner.global.exception.BusinessException;
import com.zerozoa.skinner.global.exception.ErrorCode;
import com.zerozoa.skinner.global.util.ClientUtils;
import com.zerozoa.skinner.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//인증(Authentication) 관련 API 컨트롤러
@Tag(name = "Auth API", description = "인증/토큰 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    //토큰 재발급 (Reissue)
    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용하여 Access Token을 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = getRefreshToken(request);

        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Refresh Token이 존재하지 않습니다.");
        }

        String ip = ClientUtils.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        TokenResponse tokenResponse = authService.reissue(refreshToken, ip, userAgent);

        addRefreshTokenCookie(response, tokenResponse.refreshToken());

        return ResponseEntity.ok(tokenResponse);
    }

    //쿠키에서 Refresh Token을 추출하는 메서드
    private String getRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return request.getHeader("Refresh-Token");
    }

    //Refresh Token 쿠키 설정
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure); // [수정] 환경변수로 제어 (local: false, prod: true)
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);
    }
}