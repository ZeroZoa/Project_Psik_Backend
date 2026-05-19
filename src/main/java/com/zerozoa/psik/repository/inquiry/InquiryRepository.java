package com.zerozoa.psik.repository.inquiry;

import com.zerozoa.psik.domain.inquiry.Inquiry;
import com.zerozoa.psik.domain.member.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * 1:1 문의 Repository
 */
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // 내 문의 목록 조회 — answer fetch join으로 N+1 방지
    @Query(
            value = "SELECT i FROM Inquiry i LEFT JOIN FETCH i.answer WHERE i.member.uuid = :memberUuid ORDER BY i.createdAt DESC",
            countQuery = "SELECT COUNT(i) FROM Inquiry i WHERE i.member.uuid = :memberUuid"
    )
    Page<Inquiry> findByMemberUuidWithAnswer(UUID memberUuid, Pageable pageable);

    // 관리자 전체 문의 목록 조회 — answer, member fetch join으로 N+1 방지
    @Query(
            value = "SELECT i FROM Inquiry i LEFT JOIN FETCH i.answer LEFT JOIN FETCH i.member ORDER BY i.createdAt DESC",
            countQuery = "SELECT COUNT(i) FROM Inquiry i"
    )
    Page<Inquiry> findAllWithAnswer(Pageable pageable);

    // 회원 탈퇴 시 문의 작성자를 고스트 유저로 교체
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Inquiry i SET i.member = :ghost WHERE i.member = :member")
    void anonymizeByMember(@Param("member") Member member, @Param("ghost") Member ghost);
}