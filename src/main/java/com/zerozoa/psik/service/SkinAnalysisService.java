package com.zerozoa.psik.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerozoa.psik.domain.diary.SkinAnalysis;
import com.zerozoa.psik.domain.diary.SkinDiary;
import com.zerozoa.psik.dto.diary.SkinAnalysisResponse;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.diary.SkinAnalysisRepository;

import com.zerozoa.psik.repository.diary.SkinDiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

// 피부 분석 비즈니스 로직을 담당하는 서비스
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkinAnalysisService {

    private static final int DAILY_ANALYSIS_LIMIT = 3; // 하루 최대 분석 횟수
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final SkinAnalysisRepository skinAnalysisRepository;
    private final SkinDiaryRepository skinDiaryRepository;
    private final GeminiService geminiService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    /**
     * 피부 이미지 업로드 및 Gemini AI 분석 수행
     * @param memberUuid 분석을 요청한 회원의 UUID
     * @param diaryId 분석 결과를 연결할 SkinDiary의 ID
     * @param image 분석할 피부 이미지
     * @throws BusinessException SkinDiary가 존재하지 않는 경우 {@link ErrorCode#DIARY_NOT_FOUND}
     * @throws BusinessException SkinDiary의 소유자가 아닌 경우 {@link ErrorCode#ACCESS_DENIED}
     * @throws BusinessException 이미 분석이 완료된 경우 {@link ErrorCode#ANALYSIS_ALREADY_EXISTS}
     * @throws BusinessException 하루 분석 횟수 초과 시 {@link ErrorCode#ANALYSIS_LIMIT_EXCEEDED}
     * @return SkinAnalysisResponse
     */
    @Transactional
    public SkinAnalysisResponse analyze(UUID memberUuid, Long diaryId, MultipartFile image) {

        // 다이어리 조회 + 소유자 검증
        SkinDiary skinDiary = skinDiaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        if (!skinDiary.getMember().getUuid().equals(memberUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 이미 분석 완료된 다이어리인지 확인 (재분석 불가)
        if (skinAnalysisRepository.existsBySkinDiary(skinDiary)) {
            throw new BusinessException(ErrorCode.ANALYSIS_ALREADY_EXISTS);
        }

        // 하루 분석 횟수 초과 확인
        Instant startOfDay = LocalDate.now(KST).atStartOfDay(KST).toInstant();
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);
        long todayCount = skinAnalysisRepository.countTodayAnalysisByMember(
                skinDiary.getMember(), startOfDay, endOfDay);

        if (todayCount >= DAILY_ANALYSIS_LIMIT) {
            throw new BusinessException(ErrorCode.ANALYSIS_LIMIT_EXCEEDED,
                    "하루 분석 횟수(" + DAILY_ANALYSIS_LIMIT + "회)를 초과했습니다.");
        }

        // 이미지 로컬 저장
        String imageUrl = fileStorageService.store(image, "analysis");

        // SkinAnalysis 엔티티 생성 (PENDING 상태)
        SkinAnalysis skinAnalysis = SkinAnalysis.builder()
                .skinDiary(skinDiary)
                .imageUrl(imageUrl)
                .build();
        skinAnalysisRepository.save(skinAnalysis);

        // Gemini API 호출
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(image.getInputStream())
                    .size(512, 512)
                    .outputFormat("jpg")
                    .toOutputStream(baos);
            byte[] imageBytes = baos.toByteArray();
            String mimeType = "image/jpeg";
            String resultJson = geminiService.analyzeSkin(imageBytes, mimeType);

            // JSON 파싱 후 결과 저장
            JsonNode result = objectMapper.readTree(resultJson);

            // 얼굴 감지 실패 처리
            if (result.has("error") && "FACE_NOT_DETECTED".equals(result.path("error").asText())) {
                skinAnalysis.failAnalysis();
                fileStorageService.delete(imageUrl);
                throw new BusinessException(ErrorCode.FACE_NOT_DETECTED);
            }

            skinAnalysis.completeAnalysis(
                    result.path("acne_score").asInt(),
                    result.path("wrinkle_score").asInt(),
                    result.path("tone_score").asInt(),
                    result.path("oil_score").asInt(),
                    result.path("summary").asText()
            );

            log.info("[SkinAnalysis] 분석 완료 - diaryId={}", diaryId);

        } catch (BusinessException e) {
            throw e; // BusinessException은 그대로 재던짐
        } catch (IOException e) {
            skinAnalysis.failAnalysis();
            fileStorageService.delete(imageUrl); // 추가
            log.error("[SkinAnalysis] 분석 실패 - diaryId={}, error={}", diaryId, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 분석 중 오류가 발생했습니다.");
        }

        return SkinAnalysisResponse.from(skinAnalysis);
    }

    /**
     * 다이어리의 피부 분석 결과 조회
     * @param memberUuid 조회 요청한 회원의 UUID
     * @param diaryId 조회할 SkinDiary의 ID
     * @throws BusinessException SkinDiary가 존재하지 않는 경우 {@link ErrorCode#DIARY_NOT_FOUND}
     * @throws BusinessException SkinDiary의 소유자가 아닌 경우 {@link ErrorCode#ACCESS_DENIED}
     * @throws BusinessException SkinAnalysis가 존재하지 않는 경우 {@link ErrorCode#ANALYSIS_NOT_FOUND}
     * @return SkinAnalysisResponse
     */
    public SkinAnalysisResponse getAnalysis(UUID memberUuid, Long diaryId) {

        SkinDiary skinDiary = skinDiaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        if (!skinDiary.getMember().getUuid().equals(memberUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        SkinAnalysis skinAnalysis = skinAnalysisRepository.findBySkinDiary(skinDiary)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));

        return SkinAnalysisResponse.from(skinAnalysis);
    }
}