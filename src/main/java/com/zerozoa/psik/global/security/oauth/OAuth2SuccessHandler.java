package com.zerozoa.psik.global.security.oauth;

import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.dto.auth.TokenResponse;
import com.zerozoa.psik.global.util.ClientUtils;
import com.zerozoa.psik.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 핸들러
 * 소셜 로그인(카카오/구글) 성공 시 JWT(Access/Refresh Token)를 생성하고,
 * Refresh Token은 HttpOnly 쿠키에 저장하고,
 * Access Token은 URL 파라미터로 프론트엔드에 전달 후 리다이렉트합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    //환경변수로 분리 — 프로필(local/prod)별로 다르게 설정 가능
    @Value("${app.oauth2.success-redirect-uri:http://localhost:3000/home}")
    private String webRedirectUri;

    //배포(https) 시 true로 자동 전환되도록 환경변수 사용
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    //프로필 세팅을 위한 url
    @Value("${app.oauth2.profile-setup-redirect-uri:http://localhost:3000/profile-setup}")
    private String webProfileSetupUri;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Member member = oAuth2User.getMember();

        String ip = ClientUtils.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        TokenResponse tokenResponse = authService.createToken(
                member.getUuid(),
                member.getRole().getKey(),
                ip,
                userAgent
        );

        // Refresh Token -> HttpOnly Cookie (보안 필수)
        addCookie(response, "refreshToken", tokenResponse.refreshToken(), (int)(refreshTokenExpirationMs / 1000));

        // Access Token은 URL 파라미터로 전달 (크로스 도메인 해결)
        String redirectUri = member.isProfileComplete() ? webRedirectUri : webProfileSetupUri;
        String redirectWithToken = redirectUri + "?accessToken=" + tokenResponse.accessToken();
        log.info("[OAuth2] Web Login Success. Redirecting to: {}", redirectUri);
        getRedirectStrategy().sendRedirect(request, response, redirectWithToken);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSecure ? "None" : "Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}