package com.zerozoa.skinner.dto.member;

import com.zerozoa.skinner.domain.member.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.UUID;
@Builder
@Schema(description = "회원 정보 응답")
public record MemberResponse(
        UUID uuid,
        String email,
        String nickname,
        String profileImageUrl,
        Role role,
        Gender gender,
        Integer birthYear,
        SkinType skinType,
        List<SkinConcern> skinConcerns,
        boolean profileComplete
) {
    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .uuid(member.getUuid())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .role(member.getRole())
                .gender(member.getGender())
                .birthYear(member.getBirthYear())
                .skinType(member.getSkinType())
                .skinConcerns(member.getSkinConcerns())
                .profileComplete(member.isProfileComplete())
                .build();
    }
}