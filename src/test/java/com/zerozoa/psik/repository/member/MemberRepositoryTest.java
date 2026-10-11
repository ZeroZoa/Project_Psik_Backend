package com.zerozoa.psik.repository.member;

import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * 인터페이스의 default 메서드(findByUuidOrThrow)만 검증한다.
 * findByUuid는 목으로 대체하고 default 메서드는 실제 코드를 호출한다.
 */
class MemberRepositoryTest {

    private final MemberRepository repository = mock(MemberRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("findByUuidOrThrow: 회원이 있으면 그대로 반환한다")
    void findByUuidOrThrow_found() {
        // given
        UUID uuid = UUID.randomUUID();
        Member member = mock(Member.class);
        doReturn(Optional.of(member)).when(repository).findByUuid(uuid);

        // when & then
        assertThat(repository.findByUuidOrThrow(uuid)).isSameAs(member);
    }

    @Test
    @DisplayName("findByUuidOrThrow: 회원이 없으면 MEMBER_NOT_FOUND 예외를 던진다")
    void findByUuidOrThrow_notFound() {
        // given
        UUID uuid = UUID.randomUUID();
        doReturn(Optional.empty()).when(repository).findByUuid(uuid);

        // when & then
        assertThatThrownBy(() -> repository.findByUuidOrThrow(uuid))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
