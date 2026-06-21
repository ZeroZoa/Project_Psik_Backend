package com.zerozoa.psik.domain.contents;

import com.zerozoa.psik.domain.member.SkinConcern;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnTransformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * 화장품 성분 엔티티 — 성분-화장품의 핵심 도메인
 *
 * <p>컬렉션 필드(effects, cautions, skinConcerns)는 값 타입 컬렉션으로 관리하여
 * 생명주기를 성분 엔티티에 완전히 종속시킴</p>
 *
 * <p>N+1 방지 전략:</p>
 * <ul>
 *   <li>@BatchSize: 성분 목록 조회 시 IN 쿼리로 묶어 한 번에 로딩 (N+1 → 2쿼리)</li>
 *   <li>FetchType.LAZY: 연관 데이터를 실제 사용 시점에 로딩 (불필요한 즉시 로딩 방지)</li>
 * </ul>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id") // ID 기준 객체 비교 — Set<Ingredient> 중복 방지에 필수
public class Ingredient {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** 성분 유형 (일반 화장품 / OTC 약국 / 전문의약품 / 해외직구) */
    @Enumerated(EnumType.STRING)
    private IngredientType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 효과 한 줄 요약 (목록 카드에 표시용) */
    @Column(length = 100)
    private String effectSummary;

    /**
     * 효과 태그 목록
     * 단순 텍스트 데이터이므로 값 타입 컬렉션으로 처리 (생명주기 성분에 종속)
     * @BatchSize로 N+1 방지
     */
    @BatchSize(size = 100)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ingredient_effects", joinColumns = @JoinColumn(name = "ingredient_id"))
    @Column(name = "effect")
    private List<String> effects = new ArrayList<>();

    /**
     * 주의사항 태그 목록
     * @BatchSize로 N+1 방지
     */
    @BatchSize(size = 100)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ingredient_cautions", joinColumns = @JoinColumn(name = "ingredient_id"))
    @Column(name = "caution")
    private List<String> cautions = new ArrayList<>();

    /** 연관 피부 고민 목록 — 성분 추천 필터링에서 SkinConcern.relatedTags와 매칭에 사용 */
    @BatchSize(size = 100)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ingredient_skin_concerns",
            joinColumns = @JoinColumn(name = "ingredient_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "skin_concern")
    private List<SkinConcern> skinConcerns = new ArrayList<>();

    /**
     * 이 성분이 포함된 제품 목록 (다대다 연관 관계의 주인)
     * @BatchSize로 제품 목록 조회 시 N+1 방지
     */
    @BatchSize(size = 100)
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "ingredient_product",
            joinColumns = @JoinColumn(name = "ingredient_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> products = new HashSet<>();

    /**
     * vector(768)타입으로 컬럼 지정
     */
    @Column(columnDefinition = "vector(768)")
    @ColumnTransformer(write = "CAST(? AS vector)")
    private String embedding;

    @Builder
    public Ingredient(String name, IngredientType type, String description, String effectSummary) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.effectSummary = effectSummary;
    }

    // --- 편의 메서드 ---

    public void addEffect(String effect) { this.effects.add(effect); }

    public void addCaution(String caution) { this.cautions.add(caution); }

    public void addSkinConcern(SkinConcern skinConcern) { this.skinConcerns.add(skinConcern); }

    /**
     * 제품 연결 및 양방향 동기화
     * Ingredient가 연관의 주인이므로, Product.ingredients 컬렉션에도 this를 추가
     */
    public void addProduct(Product product) {
        this.products.add(product);
        product.getIngredients().add(this);
    }

    /** 기본 정보 수정 (컬렉션 필드는 clear 후 재추가 방식 사용) */
    public void update(String name, IngredientType type,
                       String effectSummary, String description) {
        this.name = name;
        this.type = type;
        this.effectSummary = effectSummary;
        this.description = description;
    }

    public void clearSkinConcerns() { this.skinConcerns.clear(); }

    public void clearEffects() { this.effects.clear(); }

    public void clearCautions() { this.cautions.clear(); }

    public void updateEmbedding(String embedding) {
        this.embedding = embedding;
    }
}