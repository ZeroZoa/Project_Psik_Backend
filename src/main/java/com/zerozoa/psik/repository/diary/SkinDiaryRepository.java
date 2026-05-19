package com.zerozoa.psik.repository.diary;

import com.zerozoa.psik.domain.diary.SkinDiary;
import com.zerozoa.psik.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 피부 다이어리 Repository
 */
@Repository
public interface SkinDiaryRepository extends JpaRepository<SkinDiary, Long> {

    //특정 회원의 특정 날짜(자정 Instant) 다이어리 단건 조회
    Optional<SkinDiary> findByMemberAndRecordDate(Member member, Instant recordDate);

    //해당 날짜 다이어리 작성 여부 확인 (중복 작성 방지)
    boolean existsByMemberAndRecordDate(Member member, Instant recordDate);

    //특정 기간 다이어리 목록 조회 — 캘린더 뷰에 사용
    @Query("SELECT d FROM SkinDiary d WHERE d.member = :member " +
            "AND d.recordDate >= :start AND d.recordDate < :end " +
            "ORDER BY d.recordDate ASC")
    List<SkinDiary> findMonthlyDiaries(
            @Param("member") Member member,
            @Param("start") Instant start,
            @Param("end") Instant end);

    //특정 기간 다이어리 목록 조회 — UUID 기반 (서비스에서 Member 엔티티 없이 조회 시 사용)
    List<SkinDiary> findAllByMember_UuidAndRecordDateBetween(UUID memberUuid, Instant from, Instant to);

    // 회원 탈퇴 시 해당 회원의 다이어리 일괄 삭제 (cascade → SkinDiaryProduct 자동 삭제)
    @Modifying
    @Query("DELETE FROM SkinDiary sd WHERE sd.member = :member")
    void deleteAllByMember(@Param("member") Member member);

    // ElementCollection(skin_diary_diet)은 JPQL로 못 다루므로 native SQL 사용
    @Modifying
    @Query(value = "DELETE FROM skin_diary_diet WHERE skin_diary_id IN " +
            "(SELECT skin_diary_id FROM skin_diary WHERE member_id = :memberId)",
            nativeQuery = true)
    void deleteDietByMemberId(@Param("memberId") Long memberId);
}