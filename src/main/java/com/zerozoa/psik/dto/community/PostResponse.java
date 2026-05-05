package com.zerozoa.psik.dto.community;

import com.zerozoa.psik.domain.community.Post;
import com.zerozoa.psik.domain.community.PostImage;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record PostResponse(
        Long postId,
        UUID authorUuid,
        String authorNickname,
        String authorProfileImageUrl,
        String title,
        String content,
        List<String> imageUrls,     // [추가]
        int likeCount,
        int commentCount,
        int viewCount,
        boolean likedByMe,
        Instant createdAt,
        Instant updatedAt
) {
    // 목록 조회용 — 첫 번째 이미지만 (썸네일)
    public static PostResponse fromList(Post post) {
        return PostResponse.builder()
                .postId(post.getId())
                .authorUuid(post.getMember().getUuid())
                .authorNickname(post.getMember().getNickname())
                .authorProfileImageUrl(post.getMember().getProfileImageUrl())
                .title(post.getTitle())
                .content(null)
                .imageUrls(post.getImages().isEmpty()
                        ? List.of()
                        : List.of(post.getImages().get(0).getImageUrl()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .likedByMe(false)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    // 상세 조회용 — 전체 이미지
    public static PostResponse fromDetail(Post post, boolean likedByMe) {
        return PostResponse.builder()
                .postId(post.getId())
                .authorUuid(post.getMember().getUuid())
                .authorNickname(post.getMember().getNickname())
                .authorProfileImageUrl(post.getMember().getProfileImageUrl())
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrls(post.getImages().stream()
                        .map(PostImage::getImageUrl)
                        .toList())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .likedByMe(likedByMe)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}