package com.zerozoa.skinner.service;

import com.zerozoa.skinner.domain.contents.MemberProduct;
import com.zerozoa.skinner.domain.contents.Product;
import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.dto.contents.ProductDto;
import com.zerozoa.skinner.global.exception.BusinessException;
import com.zerozoa.skinner.global.exception.ErrorCode;
import com.zerozoa.skinner.repository.contents.MemberProductRepository;
import com.zerozoa.skinner.repository.contents.ProductRepository;
import com.zerozoa.skinner.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
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

    /** 샀어요 등록 (중복 불가, 취소 없음) */
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

    @Transactional
    public List<ProductDto> getOwnedProducts(UUID memberUuid) {
        return memberProductRepository.findAllByMemberUuidWithProduct(memberUuid)
                .stream()
                .map(mp -> ProductDto.from(mp.getProduct()))
                .toList();
    }

    /** 특정 제품 샀어요 여부 확인 */
    public boolean isOwned(UUID memberUuid, Long productId) {
        return memberProductRepository.existsByMember_UuidAndProduct_Id(memberUuid, productId);
    }

    /** 특정 제품 샀어요 수 */
    public long countByProduct(Long productId) {
        return memberProductRepository.countByProduct_Id(productId);
    }
}