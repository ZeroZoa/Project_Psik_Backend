package com.zerozoa.psik.repository.community;

import com.zerozoa.psik.domain.community.Post;
import com.zerozoa.psik.domain.community.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    void deleteAllByPost(Post post);
}