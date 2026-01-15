package com.zerozoa.skinner.dto.member;

import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.domain.member.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder
@Schema(description = "회원 정보 응답")
public record MemberResponse(
        UUID uuid,
        String email,
        String nickname,
        String profileImageUrl,
        Role role
) {
    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .uuid(member.getUuid())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .role(member.getRole())
                .build();
    }
}
