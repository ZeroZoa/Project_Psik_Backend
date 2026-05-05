package com.zerozoa.psik.repository.inquiry;

import com.zerozoa.psik.domain.inquiry.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // 내 문의 목록 (최신순) — answer 같이 fetch
    @Query("SELECT i FROM Inquiry i LEFT JOIN FETCH i.answer WHERE i.member.uuid = :memberUuid ORDER BY i.createdAt DESC")
    Page<Inquiry> findByMemberUuidWithAnswer(UUID memberUuid, Pageable pageable);

    // 관리자 전체 목록 (최신순) — answer 같이 fetch
    @Query("SELECT i FROM Inquiry i LEFT JOIN FETCH i.answer LEFT JOIN FETCH i.member ORDER BY i.createdAt DESC")
    Page<Inquiry> findAllWithAnswer(Pageable pageable);
}