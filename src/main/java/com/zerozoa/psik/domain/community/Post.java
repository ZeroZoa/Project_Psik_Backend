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
 * 커뮤니티 게시글 엔티티
 * likeCount, commentCount, viewCount는 반정규화된 카운터 필드로,
 * 목록 조회 시 COUNT 서브쿼리 없이 바로 사용하기 위해 엔티티에 직접 관리
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post", indexes = {
        @Index(name = "idx_post_member", columnList = "member_id"),       // 내 게시글 목록 조회
        @Index(name = "idx_post_created_at", columnList = "created_at")  // 최신순 정렬
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

    /** 반정규화 — 좋아요 수 (목록 조회 시 COUNT 쿼리 없이 바로 표시) */
    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    /** 반정규화 — 댓글 수 (목록 조회 시 COUNT 쿼리 없이 바로 표시) */
    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    /** 반정규화 — 조회 수 */
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    /** 첨부 이미지 목록 (sortOrder 오름차순 정렬) */
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

    /** 제목·본문 수정 */
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    /** 이미지 단건 추가 */
    public void addImage(PostImage image) {
        this.images.add(image);
    }

    /**
     * 이미지 전체 교체 (게시글 수정 시 사용)
     * orphanRemoval = true 설정으로 기존 이미지는 DB에서 자동 삭제됨
     */
    public void replaceImages(List<PostImage> newImages) {
        this.images.clear();
        if (newImages != null) {
            this.images.addAll(newImages);
        }
    }

    public void increaseLikeCount() { this.likeCount++; }

    /** 좋아요 수는 0 미만으로 내려가지 않도록 보호 */
    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void increaseCommentCount() { this.commentCount++; }

    /** 댓글 수는 0 미만으로 내려가지 않도록 보호 */
    public void decreaseCommentCount() {
        if (this.commentCount > 0) this.commentCount--;
    }

    public void increaseViewCount() { this.viewCount++; }

    /** 소유자 검증 — Service에서 수정·삭제 권한 확인 시 UUID로 비교 */
    public boolean isOwner(java.util.UUID memberUuid) {
        return this.member.getUuid().equals(memberUuid);
    }
}
