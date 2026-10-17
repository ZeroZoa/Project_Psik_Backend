package com.zerozoa.psik.repository.community;

import com.zerozoa.psik.domain.community.Post;
import com.zerozoa.psik.domain.member.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

    // 좋아요 처리 동안 글 행을 잠가서(SELECT ... FOR UPDATE) 같은 글의 좋아요 요청을 한 줄로 세운다
    // 트랜잭션 안에서 호출해야 하며, 락은 트랜잭션이 끝날 때(커밋/롤백) 풀린다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findByIdForUpdate(@Param("id") Long id);

    // 카운터는 엔티티에서 읽고 더하고 쓰지 않고 DB가 직접 계산한다 —
    // 동시 요청이 와도 값이 유실되지 않고, 다른 컬럼(좋아요 수 등)을 옛날 값으로 덮어쓰지도 않는다
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void increaseViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :id")
    void increaseCommentCount(@Param("id") Long id);

    // 댓글 수는 0 미만으로 내려가지 않도록 보호
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = CASE WHEN p.commentCount > :count THEN p.commentCount - :count ELSE 0 END WHERE p.id = :id")
    void decreaseCommentCount(@Param("id") Long id, @Param("count") int count);
}
