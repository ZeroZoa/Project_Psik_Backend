package com.zerozoa.psik.domain.contents;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

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

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String link;

    @Column(length = 1000)
    private String imageUrl;

    @Column(name = "owned_count", nullable = false)
    private long ownedCount = 0;

    // [변경] Ingredient 종속 제거 → 다대다 양방향 (Ingredient가 주인)
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

    public void update(String name, String brand, Long price,
                       String description, String link, String imageUrl) {
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.description = description;
        this.link = link;
        this.imageUrl = imageUrl;
    }

    public void incrementOwnedCount() {
        this.ownedCount++;
    }
}