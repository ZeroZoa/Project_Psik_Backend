package com.zerozoa.psik.domain.community;

import com.zerozoa.psik.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "comment_like",
        uniqueConstraints = {
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
