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
        name = "post_like",
        uniqueConstraints = {
                //중복방지
                @UniqueConstraint(
                        name = "uk_post_like_post_member",
                        columnNames = {"post_id", "member_id"}
                )
        }
)
public class PostLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Builder
    public PostLike(Post post, Member member) {
        this.post = post;
        this.member = member;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
