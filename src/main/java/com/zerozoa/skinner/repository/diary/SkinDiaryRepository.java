package com.zerozoa.skinner.repository.diary;

import com.zerozoa.skinner.domain.diary.SkinDiary;
import com.zerozoa.skinner.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SkinDiaryRepository extends JpaRepository<SkinDiary, Long> {

    //특정 회원의 특정 날짜(자정 Instant) 다이어리 단건 조회
    Optional<SkinDiary> findByMemberAndRecordDate(Member member, Instant recordDate);

    //이미 해당 날짜에 다이어리를 작성했는지 확인 (중복 방지용)
    boolean existsByMemberAndRecordDate(Member member, Instant recordDate);

    //특정 회원의 특정 기간 다이어리 목록 조회 (캘린더 뷰)
    @Query("SELECT d FROM SkinDiary d WHERE d.member = :member " +
            "AND d.recordDate >= :start AND d.recordDate < :end " +
            "ORDER BY d.recordDate ASC")
    List<SkinDiary> findMonthlyDiaries(
            @Param("member") Member member,
            @Param("start") Instant start,
            @Param("end") Instant end);
}