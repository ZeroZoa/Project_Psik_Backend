package com.zerozoa.psik.domain.contents;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 화장품 제품 엔티티
 * Ingredient와 다대다(N:M) 양방향 관계이며, 연관의 주인은 Ingredient
 * ownedCount는 반정규화된 필드로 MemberProduct 추가 시 Ingredient.addProduct()와 함께 증가
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id") // ID 기준 객체 비교 — Set<Product> 중복 방지에 필수
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String brand;

    private Long price;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 제품 구매 링크 */
    @Column(length = 1000)
    private String link;

    /** 제품 대표 이미지 URL */
    @Column(length = 1000)
    private String imageUrl;

    /** 반정규화 — 이 제품을 보유한 회원 수 (MemberProduct 추가 시 incrementOwnedCount() 호출) */
    @Column(name = "owned_count", nullable = false)
    private long ownedCount = 0;

    /**
     * 이 제품에 포함된 성분 목록 (다대다 양방향, 비주인 측)
     * 연관 주인은 Ingredient — 추가/삭제는 Ingredient.addProduct()를 통해 수행
     */
    @ManyToMany(mappedBy = "products")
    private Set<Ingredient> ingredients = new HashSet<>();

    @Builder
    public Product(String name, String brand, Long price, String description, String link, String imageUrl) {
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.description = description;
        this.link = link;
        this.imageUrl = imageUrl;
    }

    /** 제품 정보 전체 수정 */
    public void update(String name, String brand, Long price,
                       String description, String link, String imageUrl) {
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.description = description;
        this.link = link;
        this.imageUrl = imageUrl;
    }

    /** 보유 회원 수 증가 — MemberProduct 등록 시 호출 */
    public void incrementOwnedCount() {
        this.ownedCount++;
    }
}