package com.zerozoa.psik.service.community;

import com.zerozoa.psik.domain.community.Post;
import com.zerozoa.psik.domain.community.PostImage;
import com.zerozoa.psik.domain.community.PostLike;
import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.dto.community.PostRequest;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.community.CommentLikeRepository;
import com.zerozoa.psik.repository.community.CommentRepository;
import com.zerozoa.psik.repository.community.PostLikeRepository;
import com.zerozoa.psik.repository.community.PostRepository;
import com.zerozoa.psik.repository.member.MemberRepository;
import com.zerozoa.psik.service.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 좋아요 토글, 글 수정·삭제 시 이미지 파일 삭제 시점(커밋 후)을 검증한다.
 *
 * 좋아요 토글:
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

    // ───────────────────── 이미지 삭제 시점 ─────────────────────

    @AfterEach
    void clearSynchronization() {
        // 테스트에서 켠 트랜잭션 동기화가 다른 테스트로 새지 않게 정리
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /** @Transactional 안에 있는 것처럼 동기화를 켠다 (커밋 콜백은 triggerAfterCommit()으로 직접 호출) */
    private void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
    }

    private void triggerAfterCommit() {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
    }

    private Member ownerMember() {
        Member member = mock(Member.class);
        when(member.getUuid()).thenReturn(memberUuid);
        return member;
    }

    private Post postWithImages(Member owner, String... urls) {
        Post post = Post.builder().member(owner).title("제목").content("내용").build();
        for (int i = 0; i < urls.length; i++) {
            post.addImage(PostImage.builder().post(post).imageUrl(urls[i]).sortOrder(i).build());
        }
        return post;
    }

    private MultipartFile imageFile() {
        return mock(MultipartFile.class);
    }

    @Test
    @DisplayName("deletePost: DB 삭제를 먼저 하고, 이미지 파일은 커밋된 뒤에 지운다")
    void deletePost_deletesFilesOnlyAfterCommit() {
        // given
        Member owner = ownerMember();
        Post post = postWithImages(owner, "url-1", "url-2");
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByUuidOrThrow(memberUuid)).thenReturn(owner);
        beginTransaction();

        // when
        service.deletePost(memberUuid, POST_ID);

        // then — 커밋 전: DB 삭제는 끝났지만 파일은 그대로
        verify(postRepository).delete(post);
        verify(fileStorageService, never()).deleteAll(any());

        // 커밋 후: 파일 삭제
        triggerAfterCommit();
        verify(fileStorageService).deleteAll(List.of("url-1", "url-2"));
    }

    @Test
    @DisplayName("deletePost: 롤백되면(커밋 콜백이 호출되지 않으면) 이미지 파일을 지우지 않는다")
    void deletePost_rollback_keepsFiles() {
        // given
        Member owner = ownerMember();
        Post post = postWithImages(owner, "url-1");
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByUuidOrThrow(memberUuid)).thenReturn(owner);
        beginTransaction();

        // when — afterCommit을 호출하지 않는다 = 롤백
        service.deletePost(memberUuid, POST_ID);

        // then
        verify(fileStorageService, never()).deleteAll(any());
    }

    @Test
    @DisplayName("deletePost: 작성자도 관리자도 아니면 ACCESS_DENIED, 아무것도 지우지 않는다")
    void deletePost_notOwner_denied() {
        // given
        Member other = mock(Member.class);
        Member owner = mock(Member.class);
        when(owner.getUuid()).thenReturn(UUID.randomUUID());
        Post post = postWithImages(owner, "url-1");
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByUuidOrThrow(memberUuid)).thenReturn(other);
        beginTransaction();

        // when & then
        assertThatThrownBy(() -> service.deletePost(memberUuid, POST_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(postRepository, never()).delete(any());
        triggerAfterCommit();
        verify(fileStorageService, never()).deleteAll(any());
    }

    @Test
    @DisplayName("updatePost: 새 이미지를 먼저 올리고, 기존 파일은 커밋된 뒤에 지운다")
    void updatePost_replacesImages_deletesOldFilesAfterCommit() {
        // given
        Member owner = ownerMember();
        Post post = postWithImages(owner, "old-1", "old-2");
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByUuidOrThrow(memberUuid)).thenReturn(owner);
        when(fileStorageService.store(any(MultipartFile.class), eq("posts"))).thenReturn("new-1");
        beginTransaction();

        // when
        service.updatePost(memberUuid, POST_ID, new PostRequest("새 제목", "새 내용"), List.of(imageFile()));

        // then — 커밋 전: 새 파일은 올라갔고 기존 파일은 그대로, DB 이미지는 새것으로 교체됨
        verify(fileStorageService).store(any(MultipartFile.class), eq("posts"));
        verify(fileStorageService, never()).deleteAll(any());
        assertThat(post.getImages()).extracting(PostImage::getImageUrl).containsExactly("new-1");

        // 커밋 후: 기존 파일만 삭제
        triggerAfterCommit();
        verify(fileStorageService).deleteAll(List.of("old-1", "old-2"));
    }

    @Test
    @DisplayName("updatePost: 이미지 없이 글만 수정하면 파일을 올리지도 지우지도 않는다")
    void updatePost_textOnly_doesNotTouchFiles() {
        // given
        Member owner = ownerMember();
        Post post = postWithImages(owner, "old-1");
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByUuidOrThrow(memberUuid)).thenReturn(owner);
        beginTransaction();

        // when
        service.updatePost(memberUuid, POST_ID, new PostRequest("새 제목", "새 내용"), null);
        triggerAfterCommit();

        // then
        verifyNoInteractions(fileStorageService);
        assertThat(post.getImages()).extracting(PostImage::getImageUrl).containsExactly("old-1");
    }

    @Test
    @DisplayName("updatePost: 새 이미지 업로드가 중간에 실패하면 올라간 파일만 정리하고 기존 파일은 건드리지 않는다")
    void updatePost_uploadFails_cleansUploadedKeepsOld() {
        // given
        Member owner = ownerMember();
        Post post = postWithImages(owner, "old-1");
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(fileStorageService.store(any(MultipartFile.class), eq("posts")))
                .thenReturn("new-1")
                .thenThrow(new RuntimeException("GCS 장애"));
        beginTransaction();

        // when & then
        assertThatThrownBy(() -> service.updatePost(
                memberUuid, POST_ID, new PostRequest("제목", "내용"), List.of(imageFile(), imageFile())))
                .isInstanceOf(RuntimeException.class);

        verify(fileStorageService).deleteAll(List.of("new-1"));   // 부분 업로드 정리
        triggerAfterCommit();
        verify(fileStorageService, never()).deleteAll(List.of("old-1")); // 기존 파일은 그대로
        assertThat(post.getImages()).extracting(PostImage::getImageUrl).containsExactly("old-1");
    }

    @Test
    @DisplayName("createPost: 업로드한 이미지를 순서대로 글에 붙이고 저장한다")
    void createPost_attachesUploadedImages() {
        // given
        Member author = mock(Member.class);
        when(memberRepository.findByUuidOrThrow(memberUuid)).thenReturn(author);
        when(fileStorageService.store(any(MultipartFile.class), eq("posts"))).thenReturn("new-1", "new-2");
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        service.createPost(memberUuid, new PostRequest("제목", "내용"), List.of(imageFile(), imageFile()));

        // then
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getImages())
                .extracting(PostImage::getImageUrl, PostImage::getSortOrder)
                .containsExactly(tuple("new-1", 0), tuple("new-2", 1));
    }

    @Test
    @DisplayName("createPost: 업로드가 중간에 실패하면 이미 올라간 파일을 정리하고 글은 저장하지 않는다")
    void createPost_uploadFails_cleansUpAndDoesNotSave() {
        // given
        Member author = mock(Member.class);
        when(memberRepository.findByUuidOrThrow(memberUuid)).thenReturn(author);
        when(fileStorageService.store(any(MultipartFile.class), eq("posts")))
                .thenReturn("new-1")
                .thenThrow(new RuntimeException("GCS 장애"));

        // when & then
        assertThatThrownBy(() -> service.createPost(
                memberUuid, new PostRequest("제목", "내용"), List.of(imageFile(), imageFile())))
                .isInstanceOf(RuntimeException.class);

        verify(fileStorageService).deleteAll(List.of("new-1"));
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPost: 이미지가 5장을 넘으면 IMAGE_LIMIT_EXCEEDED, 업로드하지 않는다")
    void createPost_tooManyImages_rejected() {
        // given
        Member author = mock(Member.class);
        when(memberRepository.findByUuidOrThrow(memberUuid)).thenReturn(author);
        List<MultipartFile> six = List.of(imageFile(), imageFile(), imageFile(), imageFile(), imageFile(), imageFile());

        // when & then
        assertThatThrownBy(() -> service.createPost(memberUuid, new PostRequest("제목", "내용"), six))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        verifyNoInteractions(fileStorageService);
    }

    // ───────────────────── 조회수 ─────────────────────

    @Test
    @DisplayName("getPost: 조회수를 DB에서 먼저 올린 뒤에 글을 읽는다 (읽은 뒤 올리면 응답 조회수가 옛날 값)")
    void getPost_increasesViewCountBeforeReading() {
        // given
        Post post = postWithImages(ownerMember());
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        // when
        service.getPost(POST_ID, null);

        // then
        InOrder order = inOrder(postRepository);
        order.verify(postRepository).increaseViewCount(POST_ID);
        order.verify(postRepository).findById(POST_ID);
    }

    @Test
    @DisplayName("getPost: 글이 없으면 POST_NOT_FOUND")
    void getPost_notFound() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.getPost(POST_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }
}
