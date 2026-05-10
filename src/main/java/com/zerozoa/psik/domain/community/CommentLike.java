package com.zerozoa.psik.domain.community;

import com.zerozoa.psik.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 댓글 좋아요 엔티티
 * (comment_id, member_id) 복합 유니크 제약으로 중복 좋아요 방지
 * 좋아요 추가·취소 시 Comment.increaseLikeCount() / decreaseLikeCount() 함께 호출
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "comment_like",
        uniqueConstraints = {
                // 한 회원이 같은 댓글에 중복 좋아요 방지
                @UniqueConstraint(
                        name = "uk_comment_like_comment_member",
                        columnNames = {"comment_id", "member_id"}
                )
        }
)
public class CommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Builder
    public CommentLike(Comment comment, Member member) {
        this.comment = comment;
        this.member = member;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
