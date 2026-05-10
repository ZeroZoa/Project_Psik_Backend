package com.zerozoa.psik.domain.contents;

import com.zerozoa.psik.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 회원-제품 보유 목록 엔티티
 * 회원이 보유한 화장품을 관리하며, 피부 다이어리 작성 시 사용 제품 선택의 기반이 됨
 * (member_id, product_id) 복합 유니크 제약으로 중복 등록 방지
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member_product",
        uniqueConstraints = {
                // 한 회원이 같은 제품을 중복 등록하지 않도록 방지
                @UniqueConstraint(
                        name = "uk_member_product_member_product",
                        columnNames = {"member_id", "product_id"}
                )
        }
)
public class MemberProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Builder
    public MemberProduct(Member member, Product product) {
        this.member = member;
        this.product = product;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}