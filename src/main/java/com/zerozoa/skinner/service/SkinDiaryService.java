package com.zerozoa.skinner.service;

import com.zerozoa.skinner.domain.contents.Product;
import com.zerozoa.skinner.domain.diary.SkinDiary;
import com.zerozoa.skinner.domain.diary.SkinDiaryProduct;
import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.dto.diary.SkinDiaryRequest;
import com.zerozoa.skinner.dto.diary.SkinDiaryResponse;
import com.zerozoa.skinner.global.exception.BusinessException;
import com.zerozoa.skinner.global.exception.ErrorCode;
import com.zerozoa.skinner.repository.contents.ProductRepository;
import com.zerozoa.skinner.repository.diary.SkinDiaryRepository;
import com.zerozoa.skinner.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkinDiaryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final SkinDiaryRepository skinDiaryRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    /**
     * 다이어리 작성
     * @param memberUuid 다이어리를 생성할 회원의 UUID
     * @param request 다이어리 생성 요청 DTO
     * @throws BusinessException SkinDiary가 이미 존재하는 경우 {@link ErrorCode#DIARY_ALREADY_EXISTS} 예외 발생
     * @throws BusinessException Member가 존재하지 않는 경우 {@link ErrorCode#MEMBER_NOT_FOUND} 예외 발생
     * @return SkinDiaryResponse
     */
    @Transactional
    public SkinDiaryResponse createDiary(UUID memberUuid, SkinDiaryRequest request) {

        Member member = memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Instant normalizedInstant = normalizeToKstMidnight(request.recordDate());

        if (skinDiaryRepository.existsByMemberAndRecordDate(member, normalizedInstant)) {
            throw new BusinessException(ErrorCode.DIARY_ALREADY_EXISTS);
        }

        SkinDiary skinDiary = SkinDiary.builder()
                .member(member)
                .recordDate(normalizedInstant)
                .skinScore(request.skinScore())
                .sleepTimeMinutes(request.sleepTimeMinutes())
                .waterIntakeMl(request.waterIntakeMl())
                .diet(request.diet())
                .build();

        if (request.usedProductIds() != null && !request.usedProductIds().isEmpty()) {
            List<Product> products = productRepository.findAllById(request.usedProductIds());
            for (Product product : products) {
                SkinDiaryProduct diaryProduct = SkinDiaryProduct.builder()
                        .skinDiary(skinDiary)
                        .product(product)
                        .build();
                skinDiary.addProduct(diaryProduct);
            }
        }

        SkinDiary savedDiary = skinDiaryRepository.save(skinDiary);

        return SkinDiaryResponse.from(savedDiary);
    }

    //단건 조회 - 특정 날짜의 다이어리 보기
    /**
     * 다이어리 단건 조회
     * @param memberUuid 다이어리를 생성할 회원의 UUID
     * @param recordDate 다이어리 조회할 날짜
     * @throws BusinessException Member가 존재하지 않는 경우 {@link ErrorCode#MEMBER_NOT_FOUND} 예외 발생
     * @throws BusinessException SkinDiary가 존재하지 않는 경우 {@link ErrorCode#DIARY_NOT_FOUND} 예외 발생
     * @return SkinDiaryResponse
     */
    public SkinDiaryResponse getDiaryByDate(UUID memberUuid, Instant recordDate) {

        Member member = memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Instant normalizedInstant = normalizeToKstMidnight(recordDate);

        SkinDiary skinDiary = skinDiaryRepository.findByMemberAndRecordDate(member, normalizedInstant)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        return SkinDiaryResponse.from(skinDiary);
    }


    /**
     * 다이어리 목록 조회 - 특정 년/월의 캘린더용 다이어리 리스트
     * @param memberUuid 다이어리를 생성할 회원의 UUID
     * @param year 다이어리 조회할 연도
     * @param month 다이어리 조회할 월
     * @throws BusinessException Member가 존재하지 않는 경우 {@link ErrorCode#MEMBER_NOT_FOUND} 예외 발생
     * @return List<SkinDiaryResponse>
     */
    public List<SkinDiaryResponse> getMonthlyDiaries(UUID memberUuid, int year, int month) {

        Member member = memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        YearMonth yearMonth = YearMonth.of(year, month);

        Instant startOfMonth = yearMonth.atDay(1)
                .atStartOfDay(KST).toInstant();
        Instant startOfNextMonth = yearMonth.plusMonths(1).atDay(1)
                .atStartOfDay(KST).toInstant();

        List<SkinDiary> diaries = skinDiaryRepository
                .findMonthlyDiaries(member, startOfMonth, startOfNextMonth);

        return diaries.stream()
                .map(SkinDiaryResponse::from)
                .toList();
    }

    /**
     * 다이어리 수정
     * @param memberUuid 다이어리를 수정할 회원의 UUID
     * @param diaryId 수정할 다이어리의 diaryId
     * @param request 다이어리 수정 요청 DTO
     * @throws BusinessException SkinDiary가 존재하지 않는 경우 {@link ErrorCode#DIARY_NOT_FOUND}
     * @throws BusinessException SkinDiaryr의 소유자가 아닌 경우 {@link ErrorCode#ACCESS_DENIED}
     * @return SkinDiaryResponse
     */
    @Transactional
    public SkinDiaryResponse updateDiary(UUID memberUuid, Long diaryId, SkinDiaryRequest request) {

        SkinDiary skinDiary = skinDiaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        if (!skinDiary.getMember().getUuid().equals(memberUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        skinDiary.updateDiary(
                request.skinScore(),
                request.sleepTimeMinutes(),
                request.waterIntakeMl(),
                request.diet()
        );

        List<SkinDiaryProduct> newCosmetics = new ArrayList<>();
        if (request.usedProductIds() != null && !request.usedProductIds().isEmpty()) {
            List<Product> products = productRepository.findAllById(request.usedProductIds());
            for (Product product : products) {
                newCosmetics.add(SkinDiaryProduct.builder()
                        .skinDiary(skinDiary)
                        .product(product)
                        .build());
            }
        }
        skinDiary.updateCosmetics(newCosmetics);

        return SkinDiaryResponse.from(skinDiary);
    }

    /**
     * 다이어리 삭제
     * @param memberUuid 삭제할 다이어리 소유자의 UUID
     * @param diaryId 삭제할 다이어리의 diaryId
     * @throws BusinessException SkinDiary가 존재하지 않는 경우 {@link ErrorCode#DIARY_NOT_FOUND}
     * @throws BusinessException SkinDiaryR의 소유자가 아닌 경우 {@link ErrorCode#ACCESS_DENIED}
     */
    @Transactional
    public void deleteDiary(UUID memberUuid, Long diaryId) {

        SkinDiary skinDiary = skinDiaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        if (!skinDiary.getMember().getUuid().equals(memberUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        skinDiaryRepository.delete(skinDiary);
    }

    /**
     * 그래프용 기간별 다이어리 조회 - 최근 30일
     * @param memberUuid 다이어리를 조회할 회원의 UUID
     * @param from 다이어리 기간 조회의 시작
     * @param to 다이어리 기간 조회의 끝
     * @throws BusinessException Member가 존재하지 않는 경우 {@link ErrorCode#MEMBER_NOT_FOUND} 예외 발생
     * @return List<SkinDiaryResponse>
     */
    public List<SkinDiaryResponse> getDiariesByRange(UUID memberUuid, Instant from, Instant to) {
        Member member = memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return skinDiaryRepository.findMonthlyDiaries(member, from, to)
                .stream()
                .map(SkinDiaryResponse::from)
                .toList();
    }

    // ───────────────────── 내부 헬퍼 ─────────────────────

    /**
     * Instant를 KST 기준 자정(00:00:00.000)으로 정규화
     * 클라이언트가 어떤 시간을 보내든 해당 날짜의 자정으로 통일하여 저장
     */
    private Instant normalizeToKstMidnight(Instant instant) {
        return instant.atZone(KST)
                .toLocalDate()
                .atStartOfDay(KST)
                .toInstant();
    }
}