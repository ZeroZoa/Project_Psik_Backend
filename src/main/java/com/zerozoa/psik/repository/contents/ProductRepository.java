package com.zerozoa.psik.repository.contents;

import com.zerozoa.psik.domain.contents.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 추후 필요한 쿼리 메서드가 생기면 여기에 추가 (예: findByBrand 등)

    // 추가
    Page<Product> findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(
            String name, String brand, Pageable pageable
    );
}