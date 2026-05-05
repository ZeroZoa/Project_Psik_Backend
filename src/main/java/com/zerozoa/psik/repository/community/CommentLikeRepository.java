package com.zerozoa.psik.repository.community;

import com.zerozoa.psik.domain.community.Comment;
import com.zerozoa.psik.domain.community.CommentLike;
import com.zerozoa.psik.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    //Comment & Member조합으로 좋아요 존재 여부
    Optional<CommentLike> findByCommentAndMember(Comment comment, Member member);

    //Comment 좋아요 존재 여부 (프론트에서 좋아요 버튼 상태 표시용)
    boolean existsByCommentAndMember(Comment comment, Member member);

    //댓글 삭제 시 관련 좋아요 일괄 삭제
    void deleteAllByComment(Comment comment);
}
