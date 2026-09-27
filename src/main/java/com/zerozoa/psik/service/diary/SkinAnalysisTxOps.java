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

        // 타임아웃 처리로 이미 FAILED 전환 + 이미지 삭제까지 끝난 뒤에,
        // 인터럽트에 반응 안 한 좀비 스레드가 뒤늦게 완료 결과를 들고 도착하는 경우를 막음.
        // (이미 삭제된 이미지를 가리키는 COMPLETED 건이 생기는 걸 방지)
        if (skinAnalysis.getAnalysisStatus() != AnalysisStatus.PENDING) {
            log.warn("[SkinAnalysis] 이미 종료 처리된 건이라 완료 결과 무시 - skinAnalysisId={}, status={}",
                    skinAnalysisId, skinAnalysis.getAnalysisStatus());
            return;
        }

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
                .filter(a -> a.getAnalysisStatus() == AnalysisStatus.PENDING)
                .ifPresent(SkinAnalysis::failAnalysis);
        log.warn("[SkinAnalysis] 분석 실패 처리 - skinAnalysisId={}", skinAnalysisId);
    }
}
