package com.zerozoa.skinner.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
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
                    String uuidString = jwtTokenProvider.getPayload(token);
                    String role = jwtTokenProvider.getRole(token);

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
                } else {
                    // validateToken이 false 반환 → 만료 or 잘못된 토큰
                    // SecurityContext를 비워두면 EntryPoint가 401로 처리하게 됨
                    log.warn("[JwtFilter] 유효하지 않은 토큰 - 인증 없이 진행 (EntryPoint에서 401 처리 예정)");
                    SecurityContextHolder.clearContext();
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
