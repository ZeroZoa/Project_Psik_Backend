package com.zerozoa.psik.repository.diary;

import com.zerozoa.psik.domain.diary.SkinAnalysis;
import com.zerozoa.psik.domain.diary.SkinDiary;
import com.zerozoa.psik.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * 피부 분석 Repository
 */
public interface SkinAnalysisRepository extends JpaRepository<SkinAnalysis, Long> {

    // 다이어리로 분석 결과 조회
    Optional<SkinAnalysis> findBySkinDiary(SkinDiary skinDiary);

    // 다이어리로 분석 결과 존재 여부 확인
    boolean existsBySkinDiary(SkinDiary skinDiary);

    // 오늘 날짜 기준 회원의 분석 횟수 조회 — 일일 분석 횟수 제한 체크에 사용
    @Query("SELECT COUNT(sa) FROM SkinAnalysis sa WHERE sa.skinDiary.member = :member AND sa.createdAt >= :startOfDay AND sa.createdAt < :endOfDay")
    long countTodayAnalysisByMember(@Param("member") Member member,
                                    @Param("startOfDay") Instant startOfDay,
                                    @Param("endOfDay") Instant endOfDay);

    // 회원 탈퇴 시 해당 회원 다이어리의 분석 결과 일괄 삭제
    // SkinDiary 삭제 전 반드시 먼저 실행 (FK 제약)
    @Modifying
    @Query("DELETE FROM SkinAnalysis sa WHERE sa.skinDiary.member = :member")
    void deleteAllBySkinDiary_Member(@Param("member") Member member);
}