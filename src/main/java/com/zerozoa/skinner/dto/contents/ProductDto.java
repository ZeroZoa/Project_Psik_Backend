package com.zerozoa.skinner.dto.contents;

import com.zerozoa.skinner.domain.contents.Product;
import lombok.Builder;

@Builder
public record ProductDto(
        Long id,
        String name,
        String brand,
        Long price,
        String description,
        String imageUrl,
        String link,
        long ownedCount
) {
    public static ProductDto from(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .price(product.getPrice())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .link(product.getLink())
                .ownedCount(product.getOwnedCount())
                .build();
    }
}