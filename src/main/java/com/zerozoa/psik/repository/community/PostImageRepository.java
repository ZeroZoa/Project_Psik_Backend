package com.zerozoa.psik.repository.community;

import com.zerozoa.psik.domain.community.Post;
import com.zerozoa.psik.domain.community.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 게시글 이미지 Repository
 */
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    // 게시글 삭제 시 관련 이미지 일괄 삭제
    void deleteAllByPost(Post post);
}