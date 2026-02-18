package com.zerozoa.skinner.domain.contents;

import jakarta.persistence.*;
import lombok.*;


//특정 Ingredient 성분이 포함된 제품을 위한 엔티티
//Ingredient와 일대다 관계
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String brand;

    private Long price;

    @Column(length = 1000)
    private String link;

    @Column(length = 1000)
    private String imageUrl;

    //N+1 문제 방지: EAGER 설정 시, 제품 목록 조회만 해도 성분 조회 쿼리가 N번 추가로 발생함
    //불필요한 조인 방지 : 제품 이름만 필요한 화면에서 성분 데이터까지 가져오는 낭비 방지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @Builder
    public Product(String name, String brand, Long price, String link, String imageUrl, Ingredient ingredient) {
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.link = link;
        this.imageUrl = imageUrl;
        this.ingredient = ingredient;
    }

    //연관관계 편의 메서드
    //Ingredient 엔티티에서 호출 시 사용
    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }
}