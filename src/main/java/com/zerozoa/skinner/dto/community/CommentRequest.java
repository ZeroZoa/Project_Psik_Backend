package com.zerozoa.skinner.dto.community;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        String content,

        // 대댓글인 경우 부모 댓글 ID, 루트 댓글이면 null
        Long parentId
) {}