package com.zerozoa.psik.repository.community;

import com.zerozoa.psik.domain.community.Post;
import com.zerozoa.psik.domain.member.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
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

    // 최근 7일 내 좋아요 많은 순 top N
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.likeCount DESC, p.createdAt DESC")
    List<Post> findHotPosts(@Param("since") Instant since, Pageable pageable);

    // 최근 7일 내 조회수 높은 순 top N
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.viewCount DESC, p.createdAt DESC")
    List<Post> findPopularPosts(@Param("since") Instant since, Pageable pageable);

    // 최신 top N
    List<Post> findTop3ByOrderByCreatedAtDesc();

    // 최근 7일 좋아요 순 Page
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<Post> findHotPostsPage(@Param("since") Instant since, Pageable pageable);

    // 최근 7일 조회수 순 Page
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.viewCount DESC, p.createdAt DESC")
    Page<Post> findPopularPostsPage(@Param("since") Instant since, Pageable pageable);

    // 전체 최신순 Page
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
