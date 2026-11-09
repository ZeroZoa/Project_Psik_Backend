package com.zerozoa.psik.controller.auth;

import com.zerozoa.psik.dto.auth.TokenResponse;
import com.zerozoa.psik.service.auth.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final long REFRESH_EXPIRATION_MS = Duration.ofDays(14).toMillis();

    @Mock AuthService authService;
    @InjectMocks AuthController authController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authController, "refreshTokenExpirationMs", REFRESH_EXPIRATION_MS);
    }

    @Test
    @DisplayName("reissue: 재발급 쿠키 수명은 Refresh Token 유효기간과 같다 (7일 고정 아님)")
    void reissue_cookieMaxAgeEqualsRefreshExpiration() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "old-refresh"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authService.reissue(eq("old-refresh"), any(), any()))
                .thenReturn(new TokenResponse("new-access", "new-refresh"));

        // when
        authController.reissue(request, response);

        // then
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .contains("refreshToken=new-refresh")
                .contains("Max-Age=" + Duration.ofDays(14).toSeconds())
                .contains("SameSite=Lax");
    }
}
