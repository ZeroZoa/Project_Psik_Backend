package com.zerozoa.skinner.global.security.oauth;

import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.dto.auth.TokenResponse;
import com.zerozoa.skinner.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final AuthService authService;

    // [실무 Tip] 리다이렉트 주소는 코드가 아닌 설정 파일(yml)에서 관리합니다.
    // 개발환경(localhost:3000)과 운영환경(myapp://callback)이 다르기 때문입니다.
    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 1. 로그인된 사용자 정보 가져오기
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Member member = oAuth2User.getMember();

        // 2. 접속 정보 추출
        String ip = getIp(request);
        String userAgent = request.getHeader("User-Agent");

        // 3. 토큰 생성 (AuthService 위임)
        TokenResponse tokenResponse = authService.createToken(
                member.getUuid(),
                member.getRole().getKey(),
                ip,
                userAgent
        );

        // 4. 리다이렉트 URL 생성
        // 설정 파일에서 불러온 uri에 토큰을 쿼리 파라미터로 붙입니다.
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", tokenResponse.accessToken())
                .queryParam("refreshToken", tokenResponse.refreshToken())
                .build().toUriString();

        log.info("[OAuth2] Login Success! Member: {} -> Redirect to: {}", member.getEmail(), targetUrl);

        // 5. 리다이렉트 수행 (프론트엔드로 이동)
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // IP 추출 유틸리티
    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
