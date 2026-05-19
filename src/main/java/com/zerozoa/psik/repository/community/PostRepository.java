package com.zerozoa.psik.repository.community;

import com.zerozoa.psik.domain.community.Post;
import com.zerozoa.psik.domain.member.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * 게시글 Repository
 */
public interface PostRepository extends JpaRepository<Post, Long> {
    //특정 회원의 게시글 목록
    Page<Post> findByMemberOrderByCreatedAtDesc(Member member, Pageable pageable);

    //내가 좋아요 누른 게시글 조회
    @Query("SELECT pl.post FROM PostLike pl " +
            "WHERE pl.member = :member " +
            "ORDER BY pl.createdAt DESC")
    Page<Post> findLikedPostsByMember(@Param("member") Member member, Pageable pageable);

    //내가 댓글 쓴 게시글 조회 (중복 제거 + 최신 댓글 기준 정렬)
    @Query(
            value = "SELECT c.post FROM Comment c WHERE c.member = :member GROUP BY c.post ORDER BY MAX(c.createdAt) DESC",
            countQuery = "SELECT COUNT(DISTINCT c.post) FROM Comment c WHERE c.member = :member"
    )
    Page<Post> findCommentedPostsByMember(@Param("member") Member member, Pageable pageable);

    //키워드 검색 (제목 + 본문)
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword% ORDER BY p.createdAt DESC")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 홈 화면 위젯용 — 최근 7일 내 좋아요 많은 순 top N (Pageable로 N 조절)
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.likeCount DESC, p.createdAt DESC")
    List<Post> findHotPosts(@Param("since") Instant since, Pageable pageable);

    // 홈 화면 위젯용 — 최근 7일 내 조회수 많은 순 top N (Pageable로 N 조절)
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.viewCount DESC, p.createdAt DESC")
    List<Post> findPopularPosts(@Param("since") Instant since, Pageable pageable);

    // 홈 화면 위젯용 — 최신순 top 3 고정 반환
    List<Post> findTop3ByOrderByCreatedAtDesc();

    // 최근 7일 내 좋아요 많은 순 전체 목록
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<Post> findHotPostsPage(@Param("since") Instant since, Pageable pageable);

    // 최근 7일 내 조회수 많은 순 전체 목록
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.viewCount DESC, p.createdAt DESC")
    Page<Post> findPopularPostsPage(@Param("since") Instant since, Pageable pageable);

    // 전체 최신순 Page
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 회원 탈퇴 시 게시글 작성자를 고스트 유저로 교체
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.member = :ghost WHERE p.member = :member")
    void anonymizeByMember(@Param("member") Member member, @Param("ghost") Member ghost);
}
