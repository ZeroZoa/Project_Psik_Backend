package com.zerozoa.skinner.service;

import com.zerozoa.skinner.domain.RefreshToken;
import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.dto.auth.TokenResponse;
import com.zerozoa.skinner.global.security.JwtTokenProvider;
import com.zerozoa.skinner.repository.MemberRepository;
import com.zerozoa.skinner.repository.RefreshTokenRepository;
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
    private final MemberRepository memberRepository; // [추가] 실제 권한 조회를 위해 필요

    // 로그인 성공 시 토큰 발급
    @Transactional
    public TokenResponse createToken(UUID memberUuid, String role, String ip, String userAgent) {
        //JwtTokenProvider를 통해 AccessToken 생성
        String accessToken = jwtTokenProvider.createAccessToken(memberUuid, role);
        //JwtTokenProvider를 통해 RefreshToken 생성
        String refreshToken = jwtTokenProvider.createRefreshToken(memberUuid);

        //DB 저장 및 업데이트를 위해 현재 사용자의 UUID를 기준으로 RefreshToken을 확인
        refreshTokenRepository.findById(memberUuid)
                .ifPresentOrElse(
                        //RefreshToken이 있다면, 현재 ip, userAgent 업데이트 및 저장
                        token -> token.updateToken(refreshToken, ip, userAgent),
                        //RefreshToken이 없다면, 새로운 RefreshToken을 생성 및 저장
                        () -> refreshTokenRepository.save(new RefreshToken(memberUuid, refreshToken, ip, userAgent))
                );
        //DTO반환
        return new TokenResponse(accessToken, refreshToken);
    }

    //Access Token이 만료됐을때, Refresh Token을 이용해 새로운 토큰(Access + Refresh)을 발급
    @Transactional
    public TokenResponse reissue(String refreshToken, String ip, String userAgent) {
        //validateToken를 통해 DB조회 전 유효한 Refresh Token인지 확인 -> DB의 부하를 줄여줌
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        //토큰에서 현재 사용자의 UUID 정보를 추출
        String uuidString = jwtTokenProvider.getPayload(refreshToken);
        UUID memberUuid;

        //String형태의 UUID를 UUID객체 형태로 변환 -> 변환이 실패하면 토큰이 조작됐다고 판단
        try {
            memberUuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 토큰 정보입니다.");
        }

        //DB 조회 -> 사용자가 로그아웃했다면 재발급을 거부
        RefreshToken storedToken = refreshTokenRepository.findById(memberUuid)
                .orElseThrow(() -> new IllegalArgumentException("로그아웃 된 사용자입니다."));

        //토큰 일치 여부 확인 -> DB에 저장된 토큰과 클라이언트가 제출한 토큰이 다르면 거부
        if (!storedToken.getToken().equals(refreshToken)) {
            log.warn("[Security] Refresh Token mismatch! User: {}, IP: {}", memberUuid, ip);
            throw new IllegalArgumentException("토큰 정보가 일치하지 않습니다.");
        }

        //실제 유저의 Role 조회 (관리자 권한 유지)
        Member member = memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        String role = member.getRole().getKey(); // ROLE_USER or ROLE_ADMIN

        //정상적인 사용자로 판단하고 새로운 Access Token, Refresh Token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(memberUuid, role);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(memberUuid);

        //DB에 저장된 Refresh Token 값을 새 값으로 덮어씁니다. 추적을 위해 ip. userAgent(기기)도 저장
        storedToken.updateToken(newRefreshToken, ip, userAgent);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }
}
