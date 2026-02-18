package com.zerozoa.skinner.global.security.oauth;

import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.dto.auth.TokenResponse;
import com.zerozoa.skinner.global.util.ClientUtils;
import com.zerozoa.skinner.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 *OAuth2 로그인 성공 핸들러
 *소셜 로그인(카카오/구글)이 성공적하면 이 핸들러가 실행
 *인증된 사용자 정보를 바탕으로 JWT(Access/Refresh Token)를 생성합니다.
 *Refresh Token은 보안 쿠키(HttpOnly)에 저장하고, Access Token은 프론트엔드 리다이렉트 URL 파라미터로 전달합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    //프론트엔드 리다이렉트 URL
    //실무 배포 시에는 환경변수로 분리
    private static final String WEB_REDIRECT_URI = "http://localhost:3000/home";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        //인증된 사용자 정보 CustomOAuth2User 객체로 가져오기
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

        //Refresh Token -> HttpOnly Cookie (보안 필수, 7일)
        addCookie(response, "refreshToken", tokenResponse.refreshToken(), 7 * 24 * 60 * 60, true);

        //Access Token -> 일반 Cookie (프론트가 읽기용, 5분)
        // 자바스크립트(callback.html)가 읽어서 플러터로 넘겨줘야 하므로 httpOnly=false
        addCookie(response, "accessToken", tokenResponse.accessToken(), 5 * 60, false);

        //토큰 파라미터 없이 깨끗한 URL로 리다이렉트
        log.info("[OAuth2] Web Login Success. Redirecting to: {}", WEB_REDIRECT_URI);
        getRedirectStrategy().sendRedirect(request, response, WEB_REDIRECT_URI);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean httpOnly) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(false); // 로컬 개발(http)이므로 false. 배포(https) 시 true로 변경해야 함
        response.addCookie(cookie);
    }
}