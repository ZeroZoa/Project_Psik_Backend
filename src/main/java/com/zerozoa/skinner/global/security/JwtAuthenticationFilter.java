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
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        //토큰 유효성 검사 -> 토큰이 없거나, 유효하지 않다면 바로 필터
        if (token != null && jwtTokenProvider.validateToken(token)) {

            //토큰에서 핵심 정보 -> UUID Role 추출
            String uuidString = jwtTokenProvider.getPayload(token);
            String role = jwtTokenProvider.getRole(token);

            //Role이 없는 경우 디폴트 값 "ROLE_USER"로 설정
            if (role == null) {
                role = "ROLE_USER";
            }

            //UUID 변환 String을 UUID 객체로 변환하여 Controller에서 @AuthenticationPrincipal로 접근을 도와줌
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID in Token: {}", uuidString);
                // UUID가 깨졌다면 인증 실패 처리 -> 필터 진행시키면 뒤에서 401/403 뜸
                filterChain.doFilter(request, response);
                return;
            }

            //매 요청마다 DB를 조회하여 사용자 객체를 찾지않고, JWT의 정보를 통해 인증 객체 생성
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    uuid, // Principal: UUID 객체를 삽입
                    null,
                    Collections.singleton(new SimpleGrantedAuthority(role)) // 실제 권한 부여
            );

            //SecurityContext에 저장 -> 현재 사용자는 인증받은 사용자임을 공시
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Security Context Save - UUID: {}, Role: {}", uuid, role);
        }

        //비로그인 상태로 컨트롤러 접근(일부 기능만)
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
