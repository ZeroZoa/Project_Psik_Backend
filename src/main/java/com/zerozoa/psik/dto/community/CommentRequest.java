package com.zerozoa.psik.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        @Size(max = 1000, message = "댓글은 1000자 이내로 작성해주세요.")
        String content,

        // 대댓글인 경우 부모 댓글 ID, 루트 댓글이면 null
        Long parentId
) {}