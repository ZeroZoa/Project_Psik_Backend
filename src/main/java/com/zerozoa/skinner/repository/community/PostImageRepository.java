package com.zerozoa.skinner.repository.community;

import com.zerozoa.skinner.domain.community.Post;
import com.zerozoa.skinner.domain.community.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    void deleteAllByPost(Post post);
}