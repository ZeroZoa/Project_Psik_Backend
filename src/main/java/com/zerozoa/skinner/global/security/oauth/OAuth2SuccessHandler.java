package com.zerozoa.skinner.global.security.oauth;

import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //로그인된 사용자 정보 가져오기 (CustomOAuth2User)
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Member member = oAuth2User.getMember();

        //접속 정보(IP, User-Agent) 추출 (AuthService로 넘기기 위함)
        String ip = getIp(request);
        String userAgent = request.getHeader("User-Agent");

        //AuthService를 통해 Access/Refresh Token 생성 및 저장
        var tokenResponse = authService.createToken(
                member.getUuid(),
                member.getRole().getKey(),
                ip,
                userAgent
        );

        // 4. 프론트엔드로 리다이렉트 (토큰을 쿼리 파라미터에 실어서 보냄)
        // 실무 Tip: 프론트엔드 주소(예: localhost:3000/callback)는 환경변수로 관리하는 게 좋습니다.
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth/callback") // Flutter/프론트엔드 주소
                .queryParam("accessToken", tokenResponse.accessToken())
                .queryParam("refreshToken", tokenResponse.refreshToken())
                .build().toUriString();

        log.info("[OAuth2] Login Success! Redirect to: {}", targetUrl);

        // 5. 리다이렉트 수행
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // IP 추출 유틸리티 메서드
    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
