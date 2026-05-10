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

/**
 * 커뮤니티 댓글 엔티티
 * 자기참조(parent) 구조로 대댓글(1단계)을 지원
 * parent가 null이면 최상위(루트) 댓글, null이 아니면 대댓글
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comment", indexes = {
        @Index(name = "idx_comment_post", columnList = "post_id"),    // 게시글별 댓글 조회
        @Index(name = "idx_comment_parent", columnList = "parent_id") // 대댓글 조회
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

    /** 반정규화 — 댓글 좋아요 수 */
    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    /** 자기참조 — 대댓글 구현 (null이면 루트 댓글) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    /** 대댓글 목록 — orphanRemoval로 부모 삭제 시 자동 삭제됨 */
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

    /** 댓글 내용 수정 */
    public void updateContent(String content) {
        this.content = content;
    }

    public void increaseLikeCount() { this.likeCount++; }

    /** 좋아요 수는 0 미만으로 내려가지 않도록 보호 */
    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    /** 소유자 검증 — Service에서 수정·삭제 권한 확인 시 UUID로 비교 */
    public boolean isOwner(java.util.UUID memberUuid) {
        return this.member.getUuid().equals(memberUuid);
    }

    /** 루트 댓글 여부 확인 (parent == null이면 루트) */
    public boolean isRoot() {
        return this.parent == null;
    }
}
