package com.zerozoa.psik.repository.contents;

import com.zerozoa.psik.domain.contents.MemberProduct;
import com.zerozoa.psik.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 회원 보유 제품 Repository
 */
public interface MemberProductRepository extends JpaRepository<MemberProduct, Long> {

    // 보유 여부 확인 (중복 등록 방지)
    boolean existsByMember_UuidAndProduct_Id(UUID memberUuid, Long productId);

    // 보유 제품 단건 조회 (삭제 시 사용)
    Optional<MemberProduct> findByMember_UuidAndProduct_Id(UUID memberUuid, Long productId);

    // 내 보유 제품 목록 조회 — fetch join으로 product N+1 방지
    @Query("SELECT mp FROM MemberProduct mp JOIN FETCH mp.product WHERE mp.member.uuid = :memberUuid")
    List<MemberProduct> findAllByMemberUuidWithProduct(@Param("memberUuid") UUID memberUuid);

    long countByProduct_Id(Long productId);

    // 회원 탈퇴 시 해당 회원의 보유 제품 일괄 삭제
    @Modifying
    @Query("DELETE FROM MemberProduct mp WHERE mp.member = :member")
    void deleteAllByMember(@Param("member") Member member);
}