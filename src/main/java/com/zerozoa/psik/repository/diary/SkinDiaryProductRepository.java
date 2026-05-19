package com.zerozoa.psik.repository.diary;

import com.zerozoa.psik.domain.diary.SkinDiaryProduct;
import com.zerozoa.psik.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkinDiaryProductRepository extends JpaRepository<SkinDiaryProduct, Long> {

    // 회원 탈퇴 시 해당 회원 다이어리의 제품 기록 일괄 삭제
    @Modifying
    @Query("DELETE FROM SkinDiaryProduct sdp WHERE sdp.skinDiary.member = :member")
    void deleteAllBySkinDiary_Member(@Param("member") Member member);
}