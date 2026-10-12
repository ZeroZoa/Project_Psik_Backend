package com.zerozoa.psik.service.diary;

import com.zerozoa.psik.domain.diary.AnalysisStatus;
import com.zerozoa.psik.domain.diary.SkinAnalysis;
import com.zerozoa.psik.repository.diary.SkinAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 오래된 PENDING 분석을 FAILED로 정리하는 안전망.
 * 메시지 유실, 서버 종료 등으로 어떤 경로로든 PENDING에 고착된 건이 생기면
 * 사용자가 재업로드도 못 하는 상태(ANALYSIS_ALREADY_EXISTS)가 되므로 주기적으로 정리한다.
 *
 * 기준 시간은 워커의 처리 제한(60초)과 기한 체크(5분)보다 충분히 길게 잡아 정상 처리 중인 건을 건드리지 않는다.
 * 인스턴스가 여러 개여도 markFailed가 PENDING일 때만 상태를 바꾸므로 중복 실행은 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkinAnalysisSweeper {

    static final Duration STALE_AFTER = Duration.ofMinutes(10);

    private final SkinAnalysisRepository skinAnalysisRepository;
    private final SkinAnalysisFailureHandler failureHandler;

    @Scheduled(fixedDelay = 60_000)   // 1분마다
    public void sweepStalePending() {
        try {
            Instant threshold = Instant.now().minus(STALE_AFTER);
            List<SkinAnalysis> stale = skinAnalysisRepository
                    .findByAnalysisStatusAndCreatedAtBefore(AnalysisStatus.PENDING, threshold);

            int failed = 0;
            for (SkinAnalysis analysis : stale) {
                try {
                    failureHandler.fail(analysis.getId(), analysis.getImageUrl());
                    failed++;
                } catch (Exception e) {
                    // 한 건의 실패가 나머지 정리를 막지 않도록 건별로 처리한다
                    log.error("[Sweeper] 분석 정리 실패 - skinAnalysisId={}", analysis.getId(), e);
                }
            }

            if (failed > 0) {
                log.warn("[Sweeper] {}분 넘게 PENDING인 분석을 FAILED로 정리 - {}건", STALE_AFTER.toMinutes(), failed);
            }
        } catch (Exception e) {
            log.error("[Sweeper] 오래된 PENDING 조회 실패", e);
        }
    }
}
