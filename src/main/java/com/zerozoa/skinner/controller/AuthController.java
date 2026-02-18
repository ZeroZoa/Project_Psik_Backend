package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.dto.auth.TokenResponse;
import com.zerozoa.skinner.global.util.ClientUtils;
import com.zerozoa.skinner.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//인증(Authentication) 관련 API 컨트롤러
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    //토큰 재발급 (Reissue)
    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용하여 Access Token을 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        //Refresh Token 추출
        String refreshToken = getRefreshToken(request);

        //토큰 유효성 검사
        if (refreshToken == null) {
            return ResponseEntity.status(401).build(); // 혹은 예외 throw
        }

        //클라이언트 정보 추출
        String ip = ClientUtils.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        //토큰 재발급 서비스 호출
        TokenResponse tokenResponse = authService.reissue(refreshToken, ip, userAgent);

        //새 Refresh Token을 쿠키에 저장
        addRefreshTokenCookie(response, tokenResponse.refreshToken());

        return ResponseEntity.ok(tokenResponse);
    }

    //쿠키에서 Refresh Token을 추출하는 메서드
    private String getRefreshToken(HttpServletRequest request) {
        //쿠키에서 찾기
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                // [수정] 쿠키 이름 통일: refreshToken
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        //없으면 헤더에서 찾기
        return request.getHeader("Refresh-Token");
    }

    //Refresh Token 쿠키 설정
    //Security옵션 주의
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true); //자바스크립트 접근 불가 (XSS 방지)
        cookie.setSecure(false); //*주의* 로컬 개발(http)이므로 false. 배포(https) 시 true로 변경해야 함
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);

        // SameSite 설정은 Spring Boot 설정이나 ResponseHeader로 추가 가능 (기본적으로 Lax)
        response.addCookie(cookie);
    }
}