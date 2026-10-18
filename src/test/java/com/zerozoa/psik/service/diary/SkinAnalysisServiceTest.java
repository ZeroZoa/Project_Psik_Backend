package com.zerozoa.psik.service.diary;

import com.zerozoa.psik.domain.diary.SkinAnalysis;
import com.zerozoa.psik.domain.diary.SkinDiary;
import com.zerozoa.psik.dto.diary.SkinAnalysisRequestedEvent;
import com.zerozoa.psik.dto.diary.SkinAnalysisResponse;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.diary.SkinAnalysisRepository;
import com.zerozoa.psik.repository.diary.SkinDiaryRepository;
import com.zerozoa.psik.service.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkinAnalysisServiceTest {

    private static final Long DIARY_ID = 1L;
    private static final Long ANALYSIS_ID = 10L;
    private static final String IMAGE_URL = "https://storage.example.com/analysis/a.webp";

    @Mock SkinAnalysisRepository skinAnalysisRepository;
    @Mock SkinDiaryRepository skinDiaryRepository;
    @Mock FileStorageService fileStorageService;
    @Mock SkinAnalysisEventPublisher eventPublisher;
    @Mock SkinAnalysisFailureHandler failureHandler;
    @Mock MultipartFile image;
    @InjectMocks SkinAnalysisService service;

    private final UUID memberUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SkinDiary diary = org.mockito.Mockito.mock(SkinDiary.class);
        when(skinDiaryRepository.findOwnedById(DIARY_ID, memberUuid)).thenReturn(diary);
        when(skinAnalysisRepository.findBySkinDiary(diary)).thenReturn(Optional.empty());
        when(skinAnalysisRepository.countTodayAnalysisByMember(any(), any(), any())).thenReturn(0L);
        when(fileStorageService.store(image, "analysis")).thenReturn(IMAGE_URL);
    }

    @AfterEach
    void cleanUp() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("analyze: 트랜잭션이 활성이면 커밋 전에는 발행하지 않고, 커밋 후에 발행한다")
    void analyze_publishesOnlyAfterCommit() {
        // given
        givenSaveAssignsId();
        TransactionSynchronizationManager.initSynchronization();

        // when
        SkinAnalysisResponse response = service.analyze(memberUuid, DIARY_ID, image);

        // then: 아직 커밋 전이므로 발행되지 않는다
        assertThat(response.skinAnalysisId()).isEqualTo(ANALYSIS_ID);
        verify(eventPublisher, never()).publish(any());

        // when: 커밋 발생
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        // then
        verify(eventPublisher).publish(new SkinAnalysisRequestedEvent(ANALYSIS_ID, IMAGE_URL, "image/jpeg"));
        verifyNoInteractions(failureHandler);
    }

    @Test
    @DisplayName("analyze: 트랜잭션이 롤백되면 발행하지 않는다")
    void analyze_rollback_doesNotPublish() {
        // given
        givenSaveAssignsId();
        TransactionSynchronizationManager.initSynchronization();

        // when
        service.analyze(memberUuid, DIARY_ID, image);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        // then
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("analyze: 발행이 실패하면 FAILED 처리와 사진 정리를 요청한다")
    void analyze_publishFails_failsAnalysis() {
        // given
        givenSaveAssignsId();
        doThrow(new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)).when(eventPublisher).publish(any());

        // when (단위 테스트에는 활성 트랜잭션이 없어 AfterCommit이 즉시 실행된다)
        service.analyze(memberUuid, DIARY_ID, image);

        // then
        verify(failureHandler).fail(ANALYSIS_ID, IMAGE_URL);
    }

    @Test
    @DisplayName("analyze: 저장 중 유니크 제약을 위반하면 사진을 지우고 ANALYSIS_ALREADY_EXISTS, 발행은 하지 않는다")
    void analyze_duplicateSave_cleansUpAndNoPublish() {
        // given
        when(skinAnalysisRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

        // when & then
        assertThatThrownBy(() -> service.analyze(memberUuid, DIARY_ID, image))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ANALYSIS_ALREADY_EXISTS);
        verify(fileStorageService).delete(IMAGE_URL);
        verifyNoInteractions(eventPublisher);
    }

    private void givenSaveAssignsId() {
        when(skinAnalysisRepository.save(any())).thenAnswer(invocation -> {
            SkinAnalysis saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", ANALYSIS_ID);
            return saved;
        });
    }
}
