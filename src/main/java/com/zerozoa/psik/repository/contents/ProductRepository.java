package com.zerozoa.psik.repository.contents;

import com.zerozoa.psik.domain.contents.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 화장품 제품 Repository
 */

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 제품명 또는 브랜드명으로 검색 (대소문자 무시)
    Page<Product> findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(
            String name, String brand, Pageable pageable
    );
}