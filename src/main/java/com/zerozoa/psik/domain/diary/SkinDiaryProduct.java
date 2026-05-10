package com.zerozoa.psik.domain.diary;

import com.zerozoa.psik.domain.contents.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [스킨 다이어리 - 화장품 매핑 엔티티]
 * 다대다(N:M) 관계를 일대다(1:N) - 다대일(N:1)로 풀어내기 위한 중간 테이블입니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "skin_diary_product")
public class SkinDiaryProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skin_diary_product_id")
    private Long id;

    /** 이 기록이 속한 다이어리 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skin_diary_id", nullable = false)
    private SkinDiary skinDiary;

    /** 사용한 화장품 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Builder
    public SkinDiaryProduct(SkinDiary skinDiary, Product product) {
        this.skinDiary = skinDiary;
        this.product = product;
    }
}