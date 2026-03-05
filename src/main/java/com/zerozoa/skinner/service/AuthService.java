package com.zerozoa.skinner.service;

import com.zerozoa.skinner.domain.auth.RefreshToken;
import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.dto.auth.TokenResponse;
import com.zerozoa.skinner.global.exception.BusinessException;
import com.zerozoa.skinner.global.exception.ErrorCode;
import com.zerozoa.skinner.global.security.JwtTokenProvider;
import com.zerozoa.skinner.repository.member.MemberRepository;
import com.zerozoa.skinner.repository.auth.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

//인증/인가(Auth) 비지니스 로직을 담당하는 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;

    /**
     * 로그인 성공 시 최초 토큰 발급
     */
    @Transactional
    public TokenResponse createToken(UUID memberUuid, String role, String ip, String userAgent) {
        String accessToken = jwtTokenProvider.createAccessToken(memberUuid, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberUuid);

        refreshTokenRepository.save(new RefreshToken(memberUuid, refreshToken, ip, userAgent));

        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     * 토큰 재발급 (Refresh Token Rotation)
     */
    @Transactional
    public TokenResponse reissue(String refreshToken, String ip, String userAgent) {
        //Refresh Token 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "유효하지 않은 Refresh Token입니다.");
        }

        //DB에서 Refresh Token 값으로 조회
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "로그아웃 된 사용자이거나 유효하지 않은 토큰입니다."));

        //토큰 내 UUID 추출 및 검증
        String uuidString = jwtTokenProvider.getPayload(refreshToken);
        UUID memberUuid = UUID.fromString(uuidString);

        if (!storedToken.getMemberUuid().equals(memberUuid)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "토큰 소유자가 일치하지 않습니다.");
        }

        //회원 존재 여부 확인
        Member member = memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        //새 토큰 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(memberUuid, member.getRole().getKey());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(memberUuid);

        //Token Rotation
        storedToken.rotateToken(newRefreshToken, ip, userAgent);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃 처리
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
        log.info("[Logout] Refresh Token 삭제 완료");
    }
}