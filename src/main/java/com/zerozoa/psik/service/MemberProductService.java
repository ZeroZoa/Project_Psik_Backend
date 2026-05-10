package com.zerozoa.psik.service;

import com.zerozoa.psik.domain.contents.MemberProduct;
import com.zerozoa.psik.domain.contents.Product;
import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.dto.contents.ProductDto;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.contents.MemberProductRepository;
import com.zerozoa.psik.repository.contents.ProductRepository;
import com.zerozoa.psik.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProductService {

    private final MemberProductRepository memberProductRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    /**
     * 샀어요 등록 (중복 불가, 취소 없음)
     * @param memberUuid 샀어요를 등록할 회원의 UUID
     * @param productId 샀어요 등록할 제품의 ID
     * @throws BusinessException 이미 샀어요를 등록한 경우 {@link ErrorCode#ALREADY_OWNED_PRODUCT}
     * @throws BusinessException 회원을 찾을 수 없는 경우 {@link ErrorCode#MEMBER_NOT_FOUND}
     * @throws BusinessException 제품을 찾을 수 없는 경우 {@link ErrorCode#PRODUCT_NOT_FOUND}
     * @return 해당 제품의 샀어요 총 수
     */
    @Transactional
    public long markAsOwned(UUID memberUuid, Long productId) {
        if (memberProductRepository.existsByMember_UuidAndProduct_Id(memberUuid, productId)) {
            throw new BusinessException(ErrorCode.ALREADY_OWNED_PRODUCT);
        }

        Member member = memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.incrementOwnedCount();

        try {
            memberProductRepository.save(MemberProduct.builder()
                    .member(member)
                    .product(product)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 UniqueConstraint 위반 시 비즈니스 예외로 변환
            throw new BusinessException(ErrorCode.ALREADY_OWNED_PRODUCT);
        }

        return product.getOwnedCount();
    }

    /**
     * 회원이 샀어요 등록한 제품 목록 조회
     * @param uuid 조회할 회원의 UUID
     * @return 해당 회원이 샀어요 등록한 제품 목록
     */
    public List<ProductDto> getOwnedProducts(UUID uuid) {
        return memberProductRepository.findAllByMemberUuidWithProduct(uuid)
                .stream()
                .map(mp -> ProductDto.from(mp.getProduct()))
                .toList();
    }

    /**
     * 샀어요 여부 + 총 샀어요 수 단일 쿼리 조회
     * @param memberUuid 조회할 회원의 UUID
     * @param productId 조회할 제품의 ID
     * @return owned(샀어요 여부), count(총 샀어요 수)
     */
    public Map<String, Object> getOwnStatus(UUID memberUuid, Long productId) {
        boolean owned = memberProductRepository.existsByMember_UuidAndProduct_Id(memberUuid, productId);
        long count = memberProductRepository.countByProduct_Id(productId);
        return Map.of("owned", owned, "count", count);
    }

    /**
     * 제품 이름/브랜드 검색 (다이어리 화장품 선택용)
     * @param keyword 검색어 (null 또는 빈 문자열이면 전체 조회)
     * @param pageable 페이징 정보
     * @return 검색된 제품 목록 (Page)
     */
    public Page<ProductDto> searchProducts(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findAll(pageable).map(ProductDto::from);
        }
        return productRepository
                .findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(keyword, keyword, pageable)
                .map(ProductDto::from);
    }
}