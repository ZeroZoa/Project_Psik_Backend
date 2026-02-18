package com.zerozoa.skinner.domain.contents;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;


//성분이나 피부 고민을 나타내는 해시태그 엔티티
//Ingredient와 N:M관계(다대일-일대다 관계로 풀지 않음)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 태그 이름 (Unique Index 적용)
    //검색 성능 향상 및 중복 저장 방지
    @Column(nullable = false, unique = true)
    private String name;

    public Tag(String name) {
        this.name = name;
    }
}