package com.zerozoa.psik.repository.community;

import com.zerozoa.psik.domain.community.Post;
import com.zerozoa.psik.domain.community.PostLike;
import com.zerozoa.psik.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    //특정 회원이 특정 게시글에 좋아요했는지 확인
    Optional<PostLike> findByPostAndMember(Post post, Member member);

    //Post 좋아요 존재 여부 (프론트에서 좋아요 버튼 상태 표시용)
    boolean existsByPostAndMember(Post post, Member member);

    //게시글 삭제 시 관련 좋아요 일괄 삭제
    void deleteAllByPost(Post post);
}
