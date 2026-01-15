package com.zerozoa.skinner.global.security.oauth;

import com.zerozoa.skinner.domain.member.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {
    private final Member member; // DB에서 조회한 우리 서비스의 회원 정보
    private final Map<String, Object> attributes; // 소셜 서비스(카카오)에서 받은 원본 데이터

    public CustomOAuth2User(Member member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = attributes;
    }

    //유저의 권한 목록 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(member.getRole().getKey()));
    }

    //소셜 서비스(카카오 등)에서 받은 원본 속성값들 (JSON Map)
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // 소셜 ID가 아니라, 내부 로직 통일을 위해 'UUID'를 반환하도록 설정
    @Override
    public String getName() {
        return member.getUuid().toString();
    }

    // 편의 메소드: UUID가 필요할 때 명시적으로 호출
    public String getMemberUuid() {
        return member.getUuid().toString();
    }
}
