package com.zerozoa.psik.domain.community;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 커뮤니티 게시글 첨부 이미지 엔티티
 * Post와 1:N 관계이며, Post.replaceImages() 호출 시 orphanRemoval로 기존 이미지 자동 삭제
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_image")
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** GCS 또는 로컬에 저장된 이미지 접근 URL */
    @Column(name = "image_url", length = 1000, nullable = false)
    private String imageUrl;

    /** 이미지 표시 순서 (0부터 시작, Post에서 sortOrder ASC로 정렬됨) */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 업로드 시 원본 파일명 (디버깅·표시 용도) */
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Builder
    public PostImage(Post post, String imageUrl, int sortOrder, String originalFilename) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.originalFilename = originalFilename;
    }
}