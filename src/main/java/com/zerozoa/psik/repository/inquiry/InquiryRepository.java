package com.zerozoa.psik.repository.inquiry;

import com.zerozoa.psik.domain.inquiry.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}