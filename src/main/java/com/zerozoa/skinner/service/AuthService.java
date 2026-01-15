package com.zerozoa.skinner.service;

import com.zerozoa.skinner.domain.RefreshToken;
import com.zerozoa.skinner.dto.auth.TokenResponse;
import com.zerozoa.skinner.global.security.JwtTokenProvider;
import com.zerozoa.skinner.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    //로그인 성공 시 Access/Refresh 토큰 발급 및 저장 IP, UserAgent 정보를 받아와서 함께 기록합니다.
    @Transactional
    public TokenResponse createToken(String memberUuid, String role, String ip, String userAgent) {
        // 1. JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(memberUuid, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberUuid);

        // 2. Refresh Token DB 저장 (추적 정보 포함)
        refreshTokenRepository.findById(memberUuid)
                .ifPresentOrElse(
                        // 이미 존재하면 토큰값과 접속 정보를 업데이트
                        token -> token.updateToken(refreshToken, ip, userAgent),
                        // 없으면 새로 생성해서 저장
                        () -> refreshTokenRepository.save(new RefreshToken(memberUuid, refreshToken, ip, userAgent))
                );

        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     * 토큰 재발급 (Access Token 만료 시)
     * - 재발급 요청 시점의 IP와 UserAgent도 갱신합니다.
     */
    @Transactional
    public TokenResponse reissue(String refreshToken, String ip, String userAgent) {
        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. 토큰에서 유저 정보(UUID) 추출
        String memberUuid = jwtTokenProvider.getPayload(refreshToken);

        // 3. DB 조회 (로그아웃 여부 확인)
        RefreshToken storedToken = refreshTokenRepository.findById(memberUuid)
                .orElseThrow(() -> new IllegalArgumentException("로그아웃 된 사용자입니다."));

        // 4. 토큰 일치 여부 확인 (탈취 감지)
        if (!storedToken.getToken().equals(refreshToken)) {
            log.warn("[Security] Refresh Token mismatch! User: {}, Stored: {}, Request: {}",
                    memberUuid, storedToken.getToken(), refreshToken);
            throw new IllegalArgumentException("토큰 정보가 일치하지 않습니다.");
        }

        // 5. 새로운 토큰 쌍 발급 (RTR 방식)
        // (실무 Tip: 여기서 MemberRepository를 통해 최신 Role을 다시 가져오는 것이 더 안전합니다. 일단은 USER로 고정)
        String newAccessToken = jwtTokenProvider.createAccessToken(memberUuid, "ROLE_USER");
        String newRefreshToken = jwtTokenProvider.createRefreshToken(memberUuid);

        // 6. DB 업데이트 (토큰값 + 접속정보 갱신)
        storedToken.updateToken(newRefreshToken, ip, userAgent);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }
}
