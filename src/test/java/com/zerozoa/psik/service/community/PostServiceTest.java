package com.zerozoa.psik.service.community;

import com.zerozoa.psik.domain.community.Post;
import com.zerozoa.psik.domain.community.PostLike;
import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.community.CommentLikeRepository;
import com.zerozoa.psik.repository.community.CommentRepository;
import com.zerozoa.psik.repository.community.PostLikeRepository;
import com.zerozoa.psik.repository.community.PostRepository;
import com.zerozoa.psik.repository.member.MemberRepository;
import com.zerozoa.psik.service.storage.FileStorageService;
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
 * 좋아요 토글 로직만 검증한다.
 * 비관적 락(SELECT ... FOR UPDATE)이 실제로 걸리는지는 실제 DB 트랜잭션이 필요해 단위 테스트로 확인할 수 없고,
 * 여기서는 "락을 거는 조회(findByIdForUpdate)를 사용한다"는 것까지만 보장한다.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final Long POST_ID = 1L;

    @Mock PostRepository postRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock CommentRepository commentRepository;
    @Mock MemberRepository memberRepository;
    @Mock FileStorageService fileStorageService;
    @Mock CommentLikeRepository commentLikeRepository;
    @InjectMocks PostService service;

    private final UUID memberUuid = UUID.randomUUID();

    @Test
    @DisplayName("toggleLike: 좋아요가 없으면 락을 거는 조회로 글을 읽고, 저장하고, 좋아요 수를 올린다")
    void toggleLike_notLiked_likes() {
        // given
        Post post = mock(Post.class);
        Member member = mock(Member.class);
        when(postRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByUuidOrThrow(memberUuid)).thenReturn(member);
        when(postLikeRepository.findByPostAndMember(post, member)).thenReturn(Optional.empty());

        // when
        boolean liked = service.toggleLike(memberUuid, POST_ID);

        // then
        assertThat(liked).isTrue();
        verify(postLikeRepository).save(any(PostLike.class));
        verify(post).increaseLikeCount();
        verify(postRepository, never()).findById(any()); // 락 없는 조회로 되돌아가지 않았는지
    }

    @Test
    @DisplayName("toggleLike: 이미 좋아요했으면 삭제하고 좋아요 수를 내린다")
    void toggleLike_alreadyLiked_unlikes() {
        // given
        Post post = mock(Post.class);
        Member member = mock(Member.class);
        PostLike existing = mock(PostLike.class);
        when(postRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByUuidOrThrow(memberUuid)).thenReturn(member);
        when(postLikeRepository.findByPostAndMember(post, member)).thenReturn(Optional.of(existing));

        // when
        boolean liked = service.toggleLike(memberUuid, POST_ID);

        // then
        assertThat(liked).isFalse();
        verify(postLikeRepository).delete(existing);
        verify(post).decreaseLikeCount();
        verify(postLikeRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleLike: 글이 없으면 POST_NOT_FOUND")
    void toggleLike_postNotFound() {
        // given
        when(postRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.toggleLike(memberUuid, POST_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
        verifyNoInteractions(postLikeRepository);
    }
}
