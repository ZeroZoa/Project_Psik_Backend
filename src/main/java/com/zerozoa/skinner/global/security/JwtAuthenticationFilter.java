package com.zerozoa.skinner.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. Request Header에서 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 유효성 검사
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰에서 값 추출 (UUID, Role 등)
            // 실무 Tip: DB를 매번 조회하지 않고, 토큰에 있는 정보만으로 인증 객체를 만들어서 부하를 줄입니다.
            String uuid = jwtTokenProvider.getPayload(token);

            // 토큰에 Role 정보가 포함되어 있다면 꺼내서 넣고, 없다면 기본값(USER) 설정
            // 여기서는 단순화를 위해 ROLE_USER로 고정하거나 Provider에서 꺼내오는 방식을 씁니다.
            // (JwtTokenProvider에 getRole()을 추가하거나, DB 조회를 최소화하는 전략 선택)

            // 인증 객체 생성 (Principal: uuid)
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    uuid,
                    null,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")) // 권한 부여
            );

            // 4. SecurityContext에 저장 (이제 Spring Security는 이 요청을 '로그인 된 유저'로 인식)
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Security Context에 '{}' 인증 정보를 저장했습니다.", uuid);
        }

        filterChain.doFilter(request, response);
    }

    // 헤더에서 Bearer 토큰 꺼내기
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
