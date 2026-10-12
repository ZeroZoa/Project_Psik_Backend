package com.zerozoa.psik.service.diary;

import com.zerozoa.psik.domain.diary.AnalysisStatus;
import com.zerozoa.psik.domain.diary.SkinAnalysis;
import com.zerozoa.psik.repository.diary.SkinAnalysisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkinAnalysisSweeperTest {

    @Mock SkinAnalysisRepository skinAnalysisRepository;
    @Mock SkinAnalysisFailureHandler failureHandler;
    @InjectMocks SkinAnalysisSweeper sweeper;

    @Test
    @DisplayName("sweepStalePending: 오래된 PENDING 각각을 FAILED로 정리한다")
    void sweep_failsEachStalePending() {
        // given
        when(skinAnalysisRepository.findByAnalysisStatusAndCreatedAtBefore(eq(AnalysisStatus.PENDING), any()))
                .thenReturn(List.of(analysis(1L, "url-1"), analysis(2L, "url-2")));

        // when
        sweeper.sweepStalePending();

        // then
        verify(failureHandler).fail(1L, "url-1");
        verify(failureHandler).fail(2L, "url-2");
    }

    @Test
    @DisplayName("sweepStalePending: 기준 시각은 현재로부터 10분 전이다")
    void sweep_usesTenMinuteThreshold() {
        // given
        when(skinAnalysisRepository.findByAnalysisStatusAndCreatedAtBefore(eq(AnalysisStatus.PENDING), any()))
                .thenReturn(List.of());
        Instant before = Instant.now();

        // when
        sweeper.sweepStalePending();

        // then
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(skinAnalysisRepository).findByAnalysisStatusAndCreatedAtBefore(eq(AnalysisStatus.PENDING), captor.capture());
        Instant expected = before.minus(Duration.ofMinutes(10));
        assertThat(captor.getValue()).isBetween(expected, expected.plusSeconds(5));
    }

    @Test
    @DisplayName("sweepStalePending: 정리할 건이 없으면 아무것도 하지 않는다")
    void sweep_nothingToDo() {
        // given
        when(skinAnalysisRepository.findByAnalysisStatusAndCreatedAtBefore(eq(AnalysisStatus.PENDING), any()))
                .thenReturn(List.of());

        // when
        sweeper.sweepStalePending();

        // then
        verifyNoInteractions(failureHandler);
    }

    @Test
    @DisplayName("sweepStalePending: 한 건의 정리가 실패해도 나머지 건은 계속 정리한다")
    void sweep_oneFailureDoesNotStopOthers() {
        // given
        when(skinAnalysisRepository.findByAnalysisStatusAndCreatedAtBefore(eq(AnalysisStatus.PENDING), any()))
                .thenReturn(List.of(analysis(1L, "url-1"), analysis(2L, "url-2")));
        doThrow(new RuntimeException("db down")).when(failureHandler).fail(1L, "url-1");

        // when & then
        assertThatCode(() -> sweeper.sweepStalePending()).doesNotThrowAnyException();
        verify(failureHandler).fail(2L, "url-2");
    }

    @Test
    @DisplayName("sweepStalePending: 조회 자체가 실패해도 예외를 밖으로 던지지 않는다 (스케줄러 유지)")
    void sweep_queryFails_noThrow() {
        // given
        when(skinAnalysisRepository.findByAnalysisStatusAndCreatedAtBefore(eq(AnalysisStatus.PENDING), any()))
                .thenThrow(new RuntimeException("db down"));

        // when & then
        assertThatCode(() -> sweeper.sweepStalePending()).doesNotThrowAnyException();
        verifyNoInteractions(failureHandler);
    }

    private SkinAnalysis analysis(Long id, String imageUrl) {
        SkinAnalysis analysis = SkinAnalysis.builder().skinDiary(null).imageUrl(imageUrl).build();
        ReflectionTestUtils.setField(analysis, "id", id);
        return analysis;
    }
}
