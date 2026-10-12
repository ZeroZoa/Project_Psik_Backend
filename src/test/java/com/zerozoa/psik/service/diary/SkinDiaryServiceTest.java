package com.zerozoa.psik.service.diary;

import com.zerozoa.psik.domain.diary.SkinAnalysis;
import com.zerozoa.psik.domain.diary.SkinDiary;
import com.zerozoa.psik.repository.contents.ProductRepository;
import com.zerozoa.psik.repository.diary.SkinAnalysisRepository;
import com.zerozoa.psik.repository.diary.SkinDiaryRepository;
import com.zerozoa.psik.repository.member.MemberRepository;
import com.zerozoa.psik.service.storage.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 단위 테스트에는 활성 트랜잭션이 없으므로 AfterCommit.run은 즉시 실행된다.
 */
@ExtendWith(MockitoExtension.class)
class SkinDiaryServiceTest {

    private static final Long DIARY_ID = 1L;
    private static final String IMAGE_URL = "https://storage.example.com/analysis/a.webp";

    @Mock SkinDiaryRepository skinDiaryRepository;
    @Mock SkinAnalysisRepository skinAnalysisRepository;
    @Mock MemberRepository memberRepository;
    @Mock ProductRepository productRepository;
    @Mock FileStorageService fileStorageService;
    @InjectMocks SkinDiaryService service;

    @Test
    @DisplayName("deleteDiary: 완료된 분석이 있으면 DB 삭제 후 사진도 삭제한다")
    void deleteDiary_completedAnalysis_deletesImage() {
        // given
        UUID memberUuid = UUID.randomUUID();
        SkinDiary diary = mock(SkinDiary.class);
        SkinAnalysis analysis = analysis();
        analysis.completeAnalysis(80, 70, 60, 50, "양호");
        when(skinDiaryRepository.findOwnedById(DIARY_ID, memberUuid)).thenReturn(diary);
        when(skinAnalysisRepository.findBySkinDiary(diary)).thenReturn(Optional.of(analysis));

        // when
        service.deleteDiary(memberUuid, DIARY_ID);

        // then
        verify(skinAnalysisRepository).deleteBySkinDiary(diary);
        verify(skinDiaryRepository).delete(diary);
        verify(fileStorageService).deleteAll(List.of(IMAGE_URL));
    }

    @Test
    @DisplayName("deleteDiary: 분석 중(PENDING)인 사진도 삭제한다")
    void deleteDiary_pendingAnalysis_deletesImage() {
        // given
        UUID memberUuid = UUID.randomUUID();
        SkinDiary diary = mock(SkinDiary.class);
        when(skinDiaryRepository.findOwnedById(DIARY_ID, memberUuid)).thenReturn(diary);
        when(skinAnalysisRepository.findBySkinDiary(diary)).thenReturn(Optional.of(analysis()));

        // when
        service.deleteDiary(memberUuid, DIARY_ID);

        // then
        verify(fileStorageService).deleteAll(List.of(IMAGE_URL));
    }

    @Test
    @DisplayName("deleteDiary: 실패(FAILED)한 분석은 워커가 이미 사진을 지웠으므로 삭제 대상에서 제외한다")
    void deleteDiary_failedAnalysis_skipsImage() {
        // given
        UUID memberUuid = UUID.randomUUID();
        SkinDiary diary = mock(SkinDiary.class);
        SkinAnalysis analysis = analysis();
        analysis.failAnalysis();
        when(skinDiaryRepository.findOwnedById(DIARY_ID, memberUuid)).thenReturn(diary);
        when(skinAnalysisRepository.findBySkinDiary(diary)).thenReturn(Optional.of(analysis));

        // when
        service.deleteDiary(memberUuid, DIARY_ID);

        // then
        verify(skinAnalysisRepository).deleteBySkinDiary(diary);
        verify(fileStorageService).deleteAll(List.of());
    }

    @Test
    @DisplayName("deleteDiary: 분석이 없으면 삭제할 사진도 없다")
    void deleteDiary_noAnalysis_noImage() {
        // given
        UUID memberUuid = UUID.randomUUID();
        SkinDiary diary = mock(SkinDiary.class);
        when(skinDiaryRepository.findOwnedById(DIARY_ID, memberUuid)).thenReturn(diary);
        when(skinAnalysisRepository.findBySkinDiary(diary)).thenReturn(Optional.empty());

        // when
        service.deleteDiary(memberUuid, DIARY_ID);

        // then
        verify(skinDiaryRepository).delete(diary);
        verify(fileStorageService).deleteAll(List.of());
    }

    private SkinAnalysis analysis() {
        return SkinAnalysis.builder().skinDiary(null).imageUrl(IMAGE_URL).build();
    }
}
