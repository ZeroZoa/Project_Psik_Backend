package com.zerozoa.skinner.repository.contents;

import com.zerozoa.skinner.domain.contents.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 추후 필요한 쿼리 메서드가 생기면 여기에 추가 (예: findByBrand 등)
}