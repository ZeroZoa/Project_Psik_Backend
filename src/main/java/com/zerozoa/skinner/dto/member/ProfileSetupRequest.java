package com.zerozoa.skinner.dto.member;
import com.zerozoa.skinner.domain.member.Gender;
import com.zerozoa.skinner.domain.member.SkinConcern;
import com.zerozoa.skinner.domain.member.SkinType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.Year;
import java.util.List;

@Schema(description = "프로필 초기 설정 요청")
public record ProfileSetupRequest(

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 9, message = "닉네임은 2자 이상 9자 이하입니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9_]+$", message = "닉네임은 한글, 영문, 숫자, 언더스코어만 허용됩니다.")
        @Schema(description = "닉네임", example = "스킨케어러버")
        String nickname,

        @NotNull(message = "성별은 필수입니다.")
        @Schema(description = "성별", example = "FEMALE")
        Gender gender,

        @NotNull(message = "출생연도는 필수입니다.")
        @Min(value = 1900, message = "올바른 출생연도를 입력해주세요.")
        @Max(value = 2100, message = "올바른 출생연도를 입력해주세요.")
        @Schema(description = "출생연도", example = "1998")
        Integer birthYear,

        @NotNull(message = "피부 타입은 필수입니다.")
        @Schema(description = "피부 타입", example = "DRY")
        SkinType skinType,

        @NotEmpty(message = "피부 고민은 1개 이상 선택해야 합니다.")
        @Size(min = 1, max = 3, message = "피부 고민은 최소 1개, 최대 3개까지 선택 가능합니다.")
        List<SkinConcern> skinConcerns

) {
    // birthYear 추가 검증: 미래 연도 방지
    @AssertTrue(message = "출생연도는 현재 연도를 초과할 수 없습니다.")
    private boolean isBirthYearValid() {
        if (birthYear == null) return true;
        return birthYear <= Year.now().getValue();
    }
}