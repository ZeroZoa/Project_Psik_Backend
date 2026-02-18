package com.zerozoa.skinner.service;

import com.zerozoa.skinner.domain.auth.RefreshToken;
import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.dto.auth.TokenResponse;
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
     *로그인 성공 시 최초 토큰 발급
     *@param memberUuid 회원 식별자
     *@param role 회원 권한 (ROLE_USER 등)
     *@param ip 클라이언트 IP (보안 감사용)
     *@param userAgent 클라이언트 기기 정보 (보안 감사용)
     * @return Token DTO 객체
     */
    @Transactional
    public TokenResponse createToken(UUID memberUuid, String role, String ip, String userAgent) {
        //Access Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(memberUuid, role);
        //Refresh Token 생성
        String refreshToken = jwtTokenProvider.createRefreshToken(memberUuid);

        //Refresh Token DB저장
        //기존 토큰을 삭제하지 않고 insert -> 한 유저가 여러 기기에서 동시 로그인 가능
        refreshTokenRepository.save(new RefreshToken(memberUuid, refreshToken, ip, userAgent));

        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     *토큰 재발급
     *재발급 요청이 오면 기존 Refresh Token을 버리고, 새로운 Refresh Token을 발급합니다.
     *@param refreshToken 기존 Refresh Token
     *@param ip 클라이언트 IP (보안 감사용)
     *@param userAgent 클라이언트 기기 정보 (보안 감사용)
     * @return Token DTO 객체
     */
    @Transactional
    public TokenResponse reissue(String refreshToken, String ip, String userAgent) {
        //Refresh Token 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        //DB에서 Refresh Token 값으로 조회
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("로그아웃 된 사용자이거나 유효하지 않은 토큰입니다."));

        //토큰 내 UUID 추출 및 검증
        String uuidString = jwtTokenProvider.getPayload(refreshToken);
        UUID memberUuid = UUID.fromString(uuidString);

        if (!storedToken.getMemberUuid().equals(memberUuid)) {
            throw new IllegalArgumentException("토큰 소유자가 일치하지 않습니다.");
        }

        //회원 존재 여부 확인
        Member member = memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        //새 토큰 생성(Access, Refresh Token 둘 다)
        String newAccessToken = jwtTokenProvider.createAccessToken(memberUuid, member.getRole().getKey());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(memberUuid);

        //Token Rotation
        //기존 레코드를 업데이트하여 '사용한 토큰'을 무효화시킴
        storedToken.rotateToken(newRefreshToken, ip, userAgent);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    /**
     *로그아웃 처리
     *DB에서 해당 Refresh Token을 삭제 더 이상 재발급이 불가
     *@param refreshToken 기존 Refresh Token
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
        log.info("[Logout] Refresh Token 삭제 완료");
    }
}