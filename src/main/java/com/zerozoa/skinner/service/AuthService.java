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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;


    /**
     * 최초 토큰 발급 - 로그인
     * @param memberUuid 로그인한 Member의 UUID
     * @param role 로그인한 Member의 role - (ADMIN, USER)
     * @param ip 로그인한 Member의 현재 ip
     * @param userAgent 로그인한 Member의 현재 기기
     * @return TokenResponse
     */
    @Transactional
    public TokenResponse createToken(UUID memberUuid, String role, String ip, String userAgent) {
        //토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(memberUuid, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberUuid);

        refreshTokenRepository.save(new RefreshToken(memberUuid, refreshToken, ip, userAgent));

        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     * 토큰 재발급
     * @param refreshToken 토큰 재발급 요청한 Member의 refreshToken
     * @param ip 토큰 재발급 요청한 Member의 현재 ip
     * @param userAgent 토큰 재발급 요청한 Member의 현재 기기
     * @throws BusinessException Refresh Token이 유효하지 않는 경우 {@link ErrorCode#INVALID_TOKEN} 예외 발생
     * @throws BusinessException Refresh Token이 없는 경우 {@link ErrorCode#INVALID_TOKEN} 예외 발생
     * @throws BusinessException Refresh Token의 소유자와 현재 사용자의 UUID가 일치하지 않는 경우 {@link ErrorCode#INVALID_TOKEN} 예외 발생
     * @throws BusinessException Member가 존재하지 않는 경우 {@link ErrorCode#MEMBER_NOT_FOUND} 예외 발생
     * @return TokenResponse
     */
    @Transactional
    public TokenResponse reissue(String refreshToken, String ip, String userAgent) {

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "유효하지 않은 Refresh Token입니다.");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "로그아웃 된 사용자이거나 유효하지 않은 토큰입니다."));

        String uuidString = jwtTokenProvider.getPayload(refreshToken);
        UUID memberUuid = UUID.fromString(uuidString);

        if (!storedToken.getMemberUuid().equals(memberUuid)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "토큰 소유자가 일치하지 않습니다.");
        }

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
     * 리프레쉬 토큰 삭제 - 로그아웃
     * @param refreshToken 토큰 삭제 요청한 Member의 refreshToken
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
        log.info("[Logout] Refresh Token 삭제 완료");
    }
}