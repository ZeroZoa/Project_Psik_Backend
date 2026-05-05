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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        memberProductRepository.save(MemberProduct.builder()
                .member(member)
                .product(product)
                .build());

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
     * 특정 제품의 샀어요 등록 여부 확인
     * @param uuid 확인할 회원의 UUID
     * @param productId 확인할 제품의 ID
     * @return 샀어요 등록 시 {@code true}, 미등록 시 {@code false}
     */
    public boolean isOwned(UUID uuid, Long productId) {
        return memberProductRepository.existsByMember_UuidAndProduct_Id(uuid, productId);
    }


    /**
     * 특정 제품의 샀어요 총 수 조회
     * @param productId 조회할 제품의 ID
     * @return 해당 제품의 샀어요 총 수
     */
    public long countByProduct(Long productId) {
        return memberProductRepository.countByProduct_Id(productId);
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