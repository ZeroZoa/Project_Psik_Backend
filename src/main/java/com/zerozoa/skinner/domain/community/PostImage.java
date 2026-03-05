package com.zerozoa.skinner.domain.community;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    // 저장된 이미지 URL
    @Column(name = "image_url", length = 1000, nullable = false)
    private String imageUrl;

    // 이미지 순서 (0부터 시작)
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    // 원본 파일명
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