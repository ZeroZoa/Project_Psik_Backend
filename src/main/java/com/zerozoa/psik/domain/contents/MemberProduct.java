package com.zerozoa.psik.domain.contents;

import com.zerozoa.psik.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member_product",
        uniqueConstraints = {
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