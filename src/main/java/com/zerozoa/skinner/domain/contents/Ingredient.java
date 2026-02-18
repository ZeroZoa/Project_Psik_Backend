package com.zerozoa.skinner.domain.contents;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


//성분-화장품의 핵심 도메인
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id") // [안전장치] ID 기준으로 객체 비교 (Set 중복 방지 필수)
public class Ingredient {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private IngredientType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    //효과 리스트
    //effects는 객체일 필요없음 -> 텍스트데이터 -> 값 타입 컬렉션으로 처리하여 생명주기를 부모에게 양도(생명주기가 성분에 종속)
    //effects리스트의 테이블 이름 설정 + ingredient_id를 통해 부모 테이블 연결
    //BatchSize: 성분 목록 조회 시 N+1 문제 방지 (100개씩 묶어서 로딩)
    @BatchSize(size = 100)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ingredient_effects", joinColumns = @JoinColumn(name = "ingredient_id"))
    @Column(name = "effect")
    private List<String> effects = new ArrayList<>();

    //주의사항 리스트
    @BatchSize(size = 100)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ingredient_cautions", joinColumns = @JoinColumn(name = "ingredient_id"))
    @Column(name = "caution")
    private List<String> cautions = new ArrayList<>();

    //해시태그는 관리가 필요없는 간단한 객체 따라서 중각 테이블로 분리한 관계대신 바로 @ManyToMany사용
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "ingredient_tags",
            joinColumns = @JoinColumn(name = "ingredient_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();


    // BatchSize: 제품 목록 N+1 방지
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();


    @Builder
    public Ingredient(String name, IngredientType type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }

    // --- 편의 메서드 ---

    public void addEffect(String effect) {
        this.effects.add(effect);
    }

    public void addCaution(String caution) {
        this.cautions.add(caution);
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public void addProduct(Product product) {
        this.products.add(product);
        // [중요] 양방향 연관관계 주인(Product)에게 '나(this)'를 알려줘야 DB FK가 저장됨
        product.setIngredient(this);
    }
}

//N+1 문제는 부모 엔티티 조회 쿼리 이후, 연관된 자식 엔티티들을 사용할 때마다 추가 쿼리(N)가 발생하는 성능 이슈
//위 N+1문제를 해결하는 방법
//BatchSize -> 여러 곳 사용되는 경우 쿼리를 묶어 여러번 나갈 쿼리를 한번에 갖고오도록함
//FetchType.LAZY -> 연관된 데이터를 사용할때 그때 가져와사용(프록시 객체->가짜 객체 사용)