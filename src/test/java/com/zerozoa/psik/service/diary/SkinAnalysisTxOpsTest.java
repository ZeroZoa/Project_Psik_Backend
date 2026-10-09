package com.zerozoa.psik.service.diary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerozoa.psik.domain.diary.AnalysisStatus;
import com.zerozoa.psik.domain.diary.SkinAnalysis;
import com.zerozoa.psik.repository.diary.SkinAnalysisRepository;
import com.zerozoa.psik.service.diary.SkinAnalysisTxOps.Checkpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkinAnalysisTxOpsTest {

    private static final Long ID = 1L;
    private static final Duration DEADLINE = Duration.ofMinutes(5);

    @Mock SkinAnalysisRepository repository;
    @InjectMocks SkinAnalysisTxOps txOps;

    @Test
    @DisplayName("check: PENDING이고 기한 내면 PROCEED")
    void check_pendingWithinDeadline_proceed() {
        // given
        when(repository.findById(ID)).thenReturn(Optional.of(pendingCreatedAt(Instant.now().minusSeconds(30))));

        // when & then
        assertThat(txOps.check(ID, DEADLINE)).isEqualTo(Checkpoint.PROCEED);
    }

    @Test
    @DisplayName("check: PENDING이지만 기한을 넘기면 EXPIRED")
    void check_pendingPastDeadline_expired() {
        // given
        when(repository.findById(ID)).thenReturn(Optional.of(pendingCreatedAt(Instant.now().minus(Duration.ofMinutes(6)))));

        // when & then
        assertThat(txOps.check(ID, DEADLINE)).isEqualTo(Checkpoint.EXPIRED);
    }

    @Test
    @DisplayName("check: PENDING이 아니면 ALREADY_DONE")
    void check_notPending_alreadyDone() {
        // given
        SkinAnalysis analysis = pendingCreatedAt(Instant.now());
        analysis.failAnalysis();
        when(repository.findById(ID)).thenReturn(Optional.of(analysis));

        // when & then
        assertThat(txOps.check(ID, DEADLINE)).isEqualTo(Checkpoint.ALREADY_DONE);
    }

    @Test
    @DisplayName("markCompleted: PENDING이면 점수를 저장하고 COMPLETED로 전환")
    void markCompleted_pending_completes() throws Exception {
        // given
        SkinAnalysis analysis = pendingCreatedAt(Instant.now());
        when(repository.findById(ID)).thenReturn(Optional.of(analysis));
        var result = new ObjectMapper().readTree(
                "{\"acneScore\":80,\"wrinkleScore\":70,\"toneScore\":60,\"oilScore\":50,\"summary\":\"양호\"}");

        // when
        txOps.markCompleted(ID, result);

        // then
        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(analysis.getAcneScore()).isEqualTo(80);
        assertThat(analysis.getOilScore()).isEqualTo(50);
        assertThat(analysis.getSummary()).isEqualTo("양호");
    }

    @Test
    @DisplayName("markCompleted: 이미 FAILED면 늦게 도착한 결과를 무시 (좀비 스레드 방어)")
    void markCompleted_alreadyFailed_ignored() throws Exception {
        // given
        SkinAnalysis analysis = pendingCreatedAt(Instant.now());
        analysis.failAnalysis();
        when(repository.findById(ID)).thenReturn(Optional.of(analysis));
        var result = new ObjectMapper().readTree("{\"acneScore\":80,\"summary\":\"x\"}");

        // when
        txOps.markCompleted(ID, result);

        // then
        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getAcneScore()).isNull();
    }

    @Test
    @DisplayName("markFailed: 이미 COMPLETED면 FAILED로 덮어쓰지 않는다")
    void markFailed_alreadyCompleted_ignored() {
        // given
        SkinAnalysis analysis = pendingCreatedAt(Instant.now());
        analysis.completeAnalysis(1, 2, 3, 4, "done");
        when(repository.findById(ID)).thenReturn(Optional.of(analysis));

        // when
        txOps.markFailed(ID);

        // then
        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
    }

    // ───────── 헬퍼 ─────────

    private SkinAnalysis pendingCreatedAt(Instant createdAt) {
        SkinAnalysis analysis = SkinAnalysis.builder().skinDiary(null).imageUrl("img").build();
        ReflectionTestUtils.setField(analysis, "createdAt", createdAt);
        return analysis;
    }
}
