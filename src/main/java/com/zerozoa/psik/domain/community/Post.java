package com.zerozoa.psik.domain.community;

import com.zerozoa.psik.domain.common.BaseTimeEntity;
import com.zerozoa.psik.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post", indexes = {
        @Index(name = "idx_post_member", columnList = "member_id"),
        @Index(name = "idx_post_created_at", columnList = "created_at")
})
public class Post extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 반정규화 — 목록 조회 시 COUNT 쿼리 없이 바로 표시
    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PostImage> images = new ArrayList<>();

    @Builder
    public Post(Member member, String title, String content) {
        this.member = member;
        this.title = title;
        this.content = content;
    }

    // --- 비즈니스 메서드 ---

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    //이미지 추가
    public void addImage(PostImage image) {
        this.images.add(image);
    }

    //이미지 전체 교체 (수정 시)
    public void replaceImages(List<PostImage> newImages) {
        this.images.clear();
        if (newImages != null) {
            this.images.addAll(newImages);
        }
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        if (this.commentCount > 0) this.commentCount--;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    // 소유자 검증 — Service에서 UUID 비교용
    public boolean isOwner(java.util.UUID memberUuid) {
        return this.member.getUuid().equals(memberUuid);
    }
}
