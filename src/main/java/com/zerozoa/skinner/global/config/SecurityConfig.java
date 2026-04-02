package com.zerozoa.skinner.global.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerozoa.skinner.global.exception.ErrorCode;
import com.zerozoa.skinner.global.security.JwtAuthenticationFilter;
import com.zerozoa.skinner.global.security.oauth.CustomOAuth2UserService;
import com.zerozoa.skinner.global.security.oauth.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


import java.util.List;
import java.util.Map;

/**
 *Spring Security 설정
 *인증(Authentication) 및 인가(Authorization) 정책 설정
 *JWT 필터 등록 및 세션 정책 설정 (Stateless)
 *OAuth2 로그인 설정
 *CORS(교차 출처 리소스 공유) 설정
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    /**
     *Security Filter Chain
     *요청이 들어오면 거쳐가는 보안 필터들의 순서와 규칙을 정의
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                //CSRF 비활성화 (JWT는 세션 기반이 아니므로 불필요)
                .csrf(AbstractHttpConfigurer::disable)
                //Form Login 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                //HTTP Basic 비활성화 (JWT를 쓰므로 불필요)
                .httpBasic(AbstractHttpConfigurer::disable)
                //CORS 설정 적용 (프론트엔드 연동을 위함)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                //세션 관리 정책: STATELESS (서버에 세션을 만들지 않음 -> JWT을 통해)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // [공개 URL]
                        .requestMatchers("/", "/css/**", "/images/**", "/js/**", "/favicon.ico").permitAll()
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/api/members/check-nickname").permitAll()
                        // 관리자 전용 — ADMIN 권한 필요
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/ingredients/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        //프로필 - 인증 필요
                        .requestMatchers("/api/members/me/**").authenticated()

                        //게시글 — 마이페이지 전용 먼저 (GET /api/posts/me/** 보호)
                        .requestMatchers("/api/posts/me/**").authenticated()
                        //게시글 — GET은 공개, 나머지는 인증 필요
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers("/api/posts/**").authenticated()

                        //댓글 — GET은 공개, 나머지는 인증 필요
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/posts/*/comments").permitAll()
                        .requestMatchers("/api/posts/*/comments/**").authenticated()
                        .requestMatchers("/api/comments/**").authenticated()

                        //스킨 다이어리는 전체 인증 필요
                        .requestMatchers("/api/diaries/**").authenticated()

                        // 그 외 요청
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )

                //OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // 로그인 성공 후 처리할 핸들러 (JWT 발급 등)
                        .successHandler(oAuth2SuccessHandler)
                        // 로그인 성공 시 사용자 정보를 가져올 서비스 설정
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                )

                //인증/인가 예외 처리
                //필터 체인에서 발생하는 에러는 GlobalExceptionHandler(ControllerAdvice)까지 도달하지 못함
                //따라서 여기서 직접 JSON 응답을 생성
                .exceptionHandling(exception -> exception
                        // [401 Unauthorized] 인증되지 않은 사용자 접근 시
                        .authenticationEntryPoint((request, response, authException) -> {
                            String uri = request.getRequestURI();

                            // API 요청인 경우 JSON 에러 응답 반환
                            if (uri.startsWith("/api/")) {
                                log.warn("[API] Unauthorized: {}", authException.getMessage());
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                response.setCharacterEncoding("UTF-8");
                                //ObjectMapper로 JSON 변환
                                objectMapper.writeValue(response.getWriter(), Map.of(
                                        "timestamp", java.time.Instant.now().toString(),
                                        "status", 401,
                                        "error", "UNAUTHORIZED",
                                        "code", ErrorCode.INVALID_TOKEN.getCode(),
                                        "message", ErrorCode.INVALID_TOKEN.getMessage()
                                ));
                            }

                        // 웹 페이지 요청인 경우
                            else {
                                log.warn("[Web] Unauthorized: URI={}", uri);
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                response.setCharacterEncoding("UTF-8");
                                objectMapper.writeValue(response.getWriter(), Map.of(
                                        "timestamp", java.time.Instant.now().toString(),
                                        "status", 401,
                                        "error", "UNAUTHORIZED",
                                        "code", ErrorCode.INVALID_TOKEN.getCode(),
                                        "message", ErrorCode.INVALID_TOKEN.getMessage()
                                ));
                            }
                        })
                        // [403 Forbidden] 권한이 없는 사용자 접근 시
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("Forbidden: {}", accessDeniedException.getMessage());
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(response.getWriter(), Map.of(
                                    "timestamp", java.time.Instant.now().toString(),
                                    "status", 403,
                                    "error", "FORBIDDEN",
                                    "code", ErrorCode.ACCESS_DENIED.getCode(),
                                    "message", ErrorCode.ACCESS_DENIED.getMessage()
                            ));
                        })
                )

                //JWT 인증 필터 추가
                //UsernamePasswordAuthenticationFilter 앞에서 먼저 토큰을 검사
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     *CORS 설정
     *프론트엔드(Flutter)와 백엔드(Spring)의 도메인이 다를 때 요청을 허용하기 위함
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        //허용할 출처 (Origin)
        //localhost:3000 (프론트엔드 개발 서버)
        //List.of("http://localhost:3000", "https://skinner-app.com")
        configuration.setAllowedOriginPatterns(List.of("http://localhost:3000"));

        //허용할 메서드
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        //허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        //자격 증명(쿠키/인증헤더) 허용
        configuration.setAllowCredentials(true);

        //클라이언트(프론트)에 노출할 헤더
        //이 설정이 없으면 프론트에서 Authorization 헤더(토큰)를 읽을 수 없음
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie", "Refresh-Token"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
