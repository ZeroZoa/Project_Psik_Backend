package com.zerozoa.psik.service.diary;

import com.fasterxml.jackson.databind.JsonNode;
import com.zerozoa.psik.domain.diary.AnalysisStatus;
import com.zerozoa.psik.domain.diary.SkinAnalysis;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.diary.SkinAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SkinAnalysisWorker 전용 상태 변경 메서드 모음.
 * 각 메서드가 독립된 트랜잭션이라, Worker의 예외 처리 흐름 중간에 호출해도
 * 그 트랜잭션 자체가 별도로 커밋된다 — 예전처럼 failAnalysis()가 롤백에 같이 휩쓸리지 않음.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkinAnalysisTxOps {

    private final SkinAnalysisRepository skinAnalysisRepository;

    @Transactional(readOnly = true)
    public boolean isPending(Long skinAnalysisId) {
        return skinAnalysisRepository.findById(skinAnalysisId)
                .map(a -> a.getAnalysisStatus() == AnalysisStatus.PENDING)
                .orElse(false);
    }

    @Transactional
    public void markCompleted(Long skinAnalysisId, JsonNode result) {
        SkinAnalysis skinAnalysis = skinAnalysisRepository.findById(skinAnalysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));

        skinAnalysis.completeAnalysis(
                result.path("acneScore").asInt(),
                result.path("wrinkleScore").asInt(),
                result.path("toneScore").asInt(),
                result.path("oilScore").asInt(),
                result.path("summary").asText()
        );
        log.info("[SkinAnalysis] 분석 완료 - skinAnalysisId={}", skinAnalysisId);
    }

    @Transactional
    public void markFailed(Long skinAnalysisId) {
        skinAnalysisRepository.findById(skinAnalysisId)
                .ifPresent(SkinAnalysis::failAnalysis);
        log.warn("[SkinAnalysis] 분석 실패 처리 - skinAnalysisId={}", skinAnalysisId);
    }
}
