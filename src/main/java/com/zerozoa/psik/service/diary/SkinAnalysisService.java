package com.zerozoa.psik.service.diary;

import com.zerozoa.psik.domain.diary.AnalysisStatus;
import com.zerozoa.psik.domain.diary.SkinAnalysis;
import com.zerozoa.psik.domain.diary.SkinDiary;
import com.zerozoa.psik.dto.diary.SkinAnalysisRequestedEvent;
import com.zerozoa.psik.dto.diary.SkinAnalysisResponse;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.diary.SkinAnalysisRepository;

import com.zerozoa.psik.repository.diary.SkinDiaryRepository;
import com.zerozoa.psik.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
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
    private final FileStorageService fileStorageService;
    private final SkinAnalysisEventPublisher eventPublisher;

    /**
     * 피부 이미지 업로드 및 분석 요청 접수
     * 실제 Gemini AI 분석은 여기서 수행하지 않고 Pub/Sub으로 넘겨 비동기 처리한다({@link SkinAnalysisWorker} 참고).
     * 이 메서드는 요청을 PENDING 상태로 접수만 하고 즉시 반환한다.
     * @param memberUuid 분석을 요청한 회원의 UUID
     * @param diaryId 분석 결과를 연결할 SkinDiary의 ID
     * @param image 분석할 피부 이미지
     * @throws BusinessException SkinDiary가 존재하지 않는 경우 {@link ErrorCode#DIARY_NOT_FOUND}
     * @throws BusinessException SkinDiary의 소유자가 아닌 경우 {@link ErrorCode#ACCESS_DENIED}
     * @throws BusinessException 이미 분석이 완료되었거나 동시 요청으로 중복 생성된 경우 {@link ErrorCode#ANALYSIS_ALREADY_EXISTS}
     * @throws BusinessException 하루 분석 횟수 초과 시 {@link ErrorCode#ANALYSIS_LIMIT_EXCEEDED}
     * @return PENDING 상태의 SkinAnalysisResponse
     */
    @Transactional
    public SkinAnalysisResponse analyze(UUID memberUuid, Long diaryId, MultipartFile image) {

        // 다이어리 조회 + 소유자 검증
        SkinDiary skinDiary = skinDiaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        if (!skinDiary.getMember().getUuid().equals(memberUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 기존 분석 결과 확인 — COMPLETED/PENDING은 차단, FAILED는 재시도 허용
        Optional<SkinAnalysis> existing = skinAnalysisRepository.findBySkinDiary(skinDiary);
        if (existing.isPresent()) {
            if (existing.get().getAnalysisStatus() != AnalysisStatus.FAILED) {
                throw new BusinessException(ErrorCode.ANALYSIS_ALREADY_EXISTS);
            }
            // FAILED 건은 재시도를 위해 기존 row 제거 (unique 제약 skin_diary_id 충돌 방지 위해 즉시 flush)
            skinAnalysisRepository.delete(existing.get());
            skinAnalysisRepository.flush();
        }

        // 하루 분석 횟수 초과 확인 (이하 기존과 동일)
        Instant startOfDay = LocalDate.now(KST).atStartOfDay(KST).toInstant();
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);
        long todayCount = skinAnalysisRepository.countTodayAnalysisByMember(
                skinDiary.getMember(), startOfDay, endOfDay);

        if (todayCount >= DAILY_ANALYSIS_LIMIT) {
            throw new BusinessException(ErrorCode.ANALYSIS_LIMIT_EXCEEDED,
                    "하루 분석 횟수(" + DAILY_ANALYSIS_LIMIT + "회)를 초과했습니다.");
        }

        // 이미지 저장
        String imageUrl = fileStorageService.store(image, "analysis");

        // SkinAnalysis 엔티티 생성 (PENDING 상태)
        SkinAnalysis skinAnalysis = SkinAnalysis.builder()
                .skinDiary(skinDiary)
                .imageUrl(imageUrl)
                .build();

        // existsBySkinDiary 체크와 save 사이의 동시 요청으로
        // skin_diary_id 유니크 제약을 위반해도, 500이 아니라 비즈니스 예외로 변환
        try {
            skinAnalysisRepository.save(skinAnalysis);
        } catch (DataIntegrityViolationException e) {
            fileStorageService.delete(imageUrl);
            throw new BusinessException(ErrorCode.ANALYSIS_ALREADY_EXISTS);
        }

        // Pub/Sub 발행 실패 시에도 트랜잭션이 롤백되도록 BusinessException(unchecked)으로 전파됨
        // (SkinAnalysisEventPublisher 참고) — 여기서 이미지 파일 정리까지 같이 처리
        try {
            eventPublisher.publish(new SkinAnalysisRequestedEvent(skinAnalysis.getId(), imageUrl, "image/jpeg"));
        } catch (BusinessException e) {
            fileStorageService.delete(imageUrl);
            throw e;
        }

        log.info("[SkinAnalysis] 분석 요청 접수 - diaryId={}, skinAnalysisId={}", diaryId, skinAnalysis.getId());

        return SkinAnalysisResponse.from(skinAnalysis); // PENDING 상태로 즉시 반환
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