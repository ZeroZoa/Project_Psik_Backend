package com.zerozoa.psik.repository.community;

import com.zerozoa.psik.domain.community.Comment;
import com.zerozoa.psik.domain.community.Post;
import com.zerozoa.psik.domain.member.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    //특정 게시글의 루트 댓글만 조회(대댓글 제외) + 시간순
    //fetch join으로 N+1 방지
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.member " +
            "WHERE c.post = :post AND c.parent IS NULL " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findRootCommentsByPost(@Param("post") Post post);

    //특정 루트 댓글의 대댓글 조회
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.member " +
            "WHERE c.parent = :parent " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findChildComments(@Param("parent") Comment parent);

    //내가 작성한 댓글 전체 조회 (루트 + 대댓글 모두, 최신순)
    //fetch join으로 post, member N+1 방지
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.member " +
            "JOIN FETCH c.post " +
            "WHERE c.member = :member " +
            "ORDER BY c.createdAt DESC")
    Page<Comment> findAllByMemberOrderByCreatedAtDesc(
            @Param("member") Member member, Pageable pageable);

    //게시글 삭제 시 관련 댓글 일괄 삭제
    void deleteAllByPost(Post post);
}
