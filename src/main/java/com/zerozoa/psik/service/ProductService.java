package com.zerozoa.psik.service;

import com.zerozoa.psik.dto.contents.ProductDto;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.contents.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 제품 목록 조회
     * @param keyword 검색어 (제품명, 브랜드)
     * @param pageable 페이징 정보
     * @return 제품 목록 페이지
     */
    public Page<ProductDto> getProducts(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return productRepository
                    .findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(keyword, keyword, pageable)
                    .map(ProductDto::from);
        }
        return productRepository.findAll(pageable).map(ProductDto::from);
    }

    /**
     * 제품 단건 조회
     * @param id 제품 ID
     * @throws BusinessException 제품을 찾을 수 없는 경우 {@link ErrorCode#PRODUCT_NOT_FOUND}
     * @return 제품 DTO
     */
    public ProductDto getProduct(Long id) {
        return productRepository.findById(id)
                .map(ProductDto::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}