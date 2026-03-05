package com.zerozoa.skinner.repository.community;

import com.zerozoa.skinner.domain.community.Post;
import com.zerozoa.skinner.domain.member.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    //전체 게시글 목록 (최신순 페이징)
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    //좋아요순 페이징 조회 (같으면 최신순)
    Page<Post> findAllByOrderByLikeCountDescCreatedAtDesc(Pageable pageable);

    //조회순 페이징 조회 (같으면 최신순)
    Page<Post> findAllByOrderByViewCountDescCreatedAtDesc(Pageable pageable);

    //특정 회원의 게시글 목록
    Page<Post> findByMemberOrderByCreatedAtDesc(Member member, Pageable pageable);

    //내가 좋아요 누른 게시글 조회
    @Query("SELECT pl.post FROM PostLike pl " +
            "WHERE pl.member = :member " +
            "ORDER BY pl.createdAt DESC")
    Page<Post> findLikedPostsByMember(@Param("member") Member member, Pageable pageable);

    //내가 댓글 쓴 게시글 조회 (중복 제거 + 최신 댓글 기준 정렬)
    @Query("SELECT DISTINCT c.post FROM Comment c " +
            "WHERE c.member = :member " +
            "ORDER BY c.post.createdAt DESC")
    Page<Post> findCommentedPostsByMember(@Param("member") Member member, Pageable pageable);

    //키워드 검색 (제목 + 본문)
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword% ORDER BY p.createdAt DESC")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
