package com.zerozoa.psik.dto.member;

import com.zerozoa.psik.domain.member.SkinConcern;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SkinConcernUpdateRequest(
        @NotEmpty(message = "피부 고민은 최소 1개 이상 선택해야 합니다.")
        @Size(max = 3, message = "피부 고민은 최대 3개까지 선택 가능합니다.")
        List<SkinConcern> skinConcerns
) {}