package com.zerozoa.psik.repository.community;

import com.zerozoa.psik.domain.community.Comment;
import com.zerozoa.psik.domain.community.CommentLike;
import com.zerozoa.psik.domain.community.Post;
import com.zerozoa.psik.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

/**
 * 댓글 좋아요 Repository
 */
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    //Comment & Member조합으로 좋아요 존재 여부
    Optional<CommentLike> findByCommentAndMember(Comment comment, Member member);

    //Comment 좋아요 존재 여부 (프론트에서 좋아요 버튼 상태 표시용)
    boolean existsByCommentAndMember(Comment comment, Member member);

    //댓글 삭제 시 관련 좋아요 일괄 삭제
    void deleteAllByComment(Comment comment);

    // 특정 게시글에서 회원이 좋아요한 댓글 ID 목록 한 번에 조회
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.member = :member AND cl.comment.post = :post")
    Set<Long> findLikedCommentIdsByMemberAndPost(@Param("member") Member member, @Param("post") Post post);

    // 회원 탈퇴 시 해당 회원의 댓글 좋아요 일괄 삭제
    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.member = :member")
    void deleteAllByMember(@Param("member") Member member);
}
