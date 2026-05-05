package com.zerozoa.psik.repository.contents;

import com.zerozoa.psik.domain.contents.MemberProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberProductRepository extends JpaRepository<MemberProduct, Long> {

    boolean existsByMember_UuidAndProduct_Id(UUID memberUuid, Long productId);

    Optional<MemberProduct> findByMember_UuidAndProduct_Id(UUID memberUuid, Long productId);

    long countByProduct_Id(Long productId);

    @Query("SELECT mp FROM MemberProduct mp JOIN FETCH mp.product WHERE mp.member.uuid = :memberUuid")
    List<MemberProduct> findAllByMemberUuidWithProduct(@Param("memberUuid") UUID memberUuid);
}