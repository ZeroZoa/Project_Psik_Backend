package com.zerozoa.psik.dto.community;

import com.zerozoa.psik.domain.community.Comment;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record CommentResponse(
        Long commentId,
        UUID authorUuid,
        String authorNickname,
        String authorProfileImageUrl,
        String content,
        int likeCount,
        boolean likedByMe,
        Long parentId,
        List<CommentResponse> children,  // 대댓글 목록 (루트 댓글에만 존재)
        Instant createdAt,
        Instant updatedAt
) {
    // 단일 댓글 변환 (대댓글 없이)
    public static CommentResponse from(Comment comment, boolean likedByMe) {
        return CommentResponse.builder()
                .commentId(comment.getId())
                .authorUuid(comment.getMember().getUuid())
                .authorNickname(comment.getMember().getNickname())
                .authorProfileImageUrl(comment.getMember().getProfileImageUrl())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .likedByMe(likedByMe)
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .children(List.of())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    // 루트 댓글 + 대댓글 포함 변환
    public static CommentResponse fromWithChildren(
            Comment comment, boolean likedByMe, List<CommentResponse> children
    ) {
        return CommentResponse.builder()
                .commentId(comment.getId())
                .authorUuid(comment.getMember().getUuid())
                .authorNickname(comment.getMember().getNickname())
                .authorProfileImageUrl(comment.getMember().getProfileImageUrl())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .likedByMe(likedByMe)
                .parentId(null)
                .children(children)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}