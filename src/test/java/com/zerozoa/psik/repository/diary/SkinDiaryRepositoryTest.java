package com.zerozoa.psik.repository.diary;

import com.zerozoa.psik.domain.diary.SkinDiary;
import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 인터페이스의 default 메서드(findOwnedById)만 검증한다.
 * findById는 목으로 대체하고 default 메서드는 실제 코드를 호출한다.
 */
class SkinDiaryRepositoryTest {

    private static final Long DIARY_ID = 1L;

    private final SkinDiaryRepository repository = mock(SkinDiaryRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("findOwnedById: 내 다이어리면 그대로 반환한다")
    void findOwnedById_owner() {
        // given
        UUID ownerUuid = UUID.randomUUID();
        SkinDiary diary = diaryOwnedBy(ownerUuid);
        doReturn(Optional.of(diary)).when(repository).findById(DIARY_ID);

        // when & then
        assertThat(repository.findOwnedById(DIARY_ID, ownerUuid)).isSameAs(diary);
    }

    @Test
    @DisplayName("findOwnedById: 다이어리가 없으면 DIARY_NOT_FOUND")
    void findOwnedById_notFound() {
        // given
        doReturn(Optional.empty()).when(repository).findById(DIARY_ID);

        // when & then
        assertThatThrownBy(() -> repository.findOwnedById(DIARY_ID, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DIARY_NOT_FOUND);
    }

    @Test
    @DisplayName("findOwnedById: 다른 사람의 다이어리면 ACCESS_DENIED")
    void findOwnedById_notOwner() {
        // given
        SkinDiary diary = diaryOwnedBy(UUID.randomUUID());
        doReturn(Optional.of(diary)).when(repository).findById(DIARY_ID);

        // when & then
        assertThatThrownBy(() -> repository.findOwnedById(DIARY_ID, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    private SkinDiary diaryOwnedBy(UUID ownerUuid) {
        Member owner = mock(Member.class);
        when(owner.getUuid()).thenReturn(ownerUuid);
        return SkinDiary.builder()
                .member(owner)
                .recordDate(Instant.now())
                .skinScore(50)
                .build();
    }
}
