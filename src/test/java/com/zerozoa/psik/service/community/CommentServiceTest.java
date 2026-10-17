package com.zerozoa.psik.service.community;

import com.zerozoa.psik.domain.community.Comment;
import com.zerozoa.psik.domain.community.CommentLike;
import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.community.CommentLikeRepository;
import com.zerozoa.psik.repository.community.CommentRepository;
import com.zerozoa.psik.repository.community.PostRepository;
import com.zerozoa.psik.repository.member.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 댓글 좋아요 토글 로직만 검증한다.
 * 비관적 락이 실제로 걸리는지는 실제 DB 트랜잭션이 필요해 단위 테스트로 확인할 수 없고,
 * 여기서는 "락을 거는 조회(findByIdForUpdate)를 사용한다"는 것까지만 보장한다.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final Long COMMENT_ID = 1L;

    @Mock CommentRepository commentRepository;
    @Mock CommentLikeRepository commentLikeRepository;
    @Mock PostRepository postRepository;
    @Mock MemberRepository memberRepository;
    @InjectMocks CommentService service;

    private final UUID memberUuid = UUID.randomUUID();

    @Test
    @DisplayName("toggleLike: 좋아요가 없으면 락을 거는 조회로 댓글을 읽고, 저장하고, 좋아요 수를 올린다")
    void toggleLike_notLiked_likes() {
        // given
        Comment comment = mock(Comment.class);
        Member member = mock(Member.class);
        when(commentRepository.findByIdForUpdate(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(memberRepository.findByUuidOrThrow(memberUuid)).thenReturn(member);
        when(commentLikeRepository.findByCommentAndMember(comment, member)).thenReturn(Optional.empty());

        // when
        boolean liked = service.toggleLike(memberUuid, COMMENT_ID);

        // then
        assertThat(liked).isTrue();
        verify(commentLikeRepository).save(any(CommentLike.class));
        verify(comment).increaseLikeCount();
        verify(commentRepository, never()).findById(any()); // 락 없는 조회로 되돌아가지 않았는지
    }

    @Test
    @DisplayName("toggleLike: 이미 좋아요했으면 삭제하고 좋아요 수를 내린다")
    void toggleLike_alreadyLiked_unlikes() {
        // given
        Comment comment = mock(Comment.class);
        Member member = mock(Member.class);
        CommentLike existing = mock(CommentLike.class);
        when(commentRepository.findByIdForUpdate(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(memberRepository.findByUuidOrThrow(memberUuid)).thenReturn(member);
        when(commentLikeRepository.findByCommentAndMember(comment, member)).thenReturn(Optional.of(existing));

        // when
        boolean liked = service.toggleLike(memberUuid, COMMENT_ID);

        // then
        assertThat(liked).isFalse();
        verify(commentLikeRepository).delete(existing);
        verify(comment).decreaseLikeCount();
        verify(commentLikeRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleLike: 댓글이 없으면 COMMENT_NOT_FOUND")
    void toggleLike_commentNotFound() {
        // given
        when(commentRepository.findByIdForUpdate(COMMENT_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.toggleLike(memberUuid, COMMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        verifyNoInteractions(commentLikeRepository);
    }
}
