package com.zerozoa.skinner.domain.community;

import com.zerozoa.skinner.domain.common.BaseTimeEntity;
import com.zerozoa.skinner.domain.member.Member;
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
@Table(name = "comment", indexes = {
        @Index(name = "idx_comment_post", columnList = "post_id"),
        @Index(name = "idx_comment_parent", columnList = "parent_id")
})
public class Comment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 반정규화 — 댓글 좋아요 수
    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    // 자기참조 — 대댓글 구현
    // parent가 null이면 최상위(루트) 댓글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 대댓글 목록 (읽기 전용)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();

    @Builder
    public Comment(Post post, Member member, String content, Comment parent) {
        this.post = post;
        this.member = member;
        this.content = content;
        this.parent = parent;
    }

    // --- 비즈니스 메서드 ---

    public void updateContent(String content) {
        this.content = content;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public boolean isOwner(java.util.UUID memberUuid) {
        return this.member.getUuid().equals(memberUuid);
    }

    // 루트 댓글인지 확인
    public boolean isRoot() {
        return this.parent == null;
    }
}
