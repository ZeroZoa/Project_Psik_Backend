package com.zerozoa.psik.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            // 만료/서명 오류 등 JWT 예외를 직접 캐치하여 401로 응답
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    Claims claims = jwtTokenProvider.parseClaims(token);

                    // RefreshToken이 Authorization 헤더로 들어와 Access Token처럼 인증되는 것을 차단
                    if (!JwtTokenProvider.TYPE_ACCESS.equals(claims.get("typ", String.class))) {
                        log.warn("[JwtFilter] Access Token이 아닌 토큰으로 인증 시도 - 401 응답");
                        sendUnauthorizedResponse(response, "유효하지 않은 토큰입니다.");
                        return;
                    }

                    String uuidString = claims.getSubject();
                    String role = claims.get("role", String.class);

                    if (role == null) {
                        role = "ROLE_USER";
                    }

                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidString);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid UUID in Token: {}", uuidString);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            uuid,
                            null,
                            Collections.singleton(new SimpleGrantedAuthority(role))
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Security Context Save - UUID: {}, Role: {}", uuid, role);
                }
            } catch (JwtException e) {
                log.warn("[JwtFilter] 유효하지 않은 JWT 토큰 - 401 응답: {}", e.getMessage());
                sendUnauthorizedResponse(response, "유효하지 않은 토큰입니다.");
                return;
            } catch (Exception e) {
                log.warn("[JwtFilter] 토큰 처리 오류 - 401 응답: {}", e.getMessage());
                sendUnauthorizedResponse(response, "토큰 처리 중 오류가 발생했습니다.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", "INVALID_TOKEN",
                "message", message
        ));
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
