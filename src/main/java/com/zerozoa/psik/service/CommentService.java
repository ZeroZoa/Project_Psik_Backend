package com.zerozoa.psik.service;

import com.zerozoa.psik.domain.community.Comment;
import com.zerozoa.psik.domain.community.CommentLike;
import com.zerozoa.psik.domain.community.Post;
import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.dto.community.CommentRequest;
import com.zerozoa.psik.dto.community.CommentResponse;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.community.CommentLikeRepository;
import com.zerozoa.psik.repository.community.CommentRepository;
import com.zerozoa.psik.repository.community.PostRepository;
import com.zerozoa.psik.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;


    /**
     * 댓글 작성 (루트 댓글 또는 대댓글)
     * @param memberUuid 댓글을 작성한 Member의 UUID
     * @param postId 댓글이 작성될 Post의 ID
     * @param request 댓글 작성 요청 DTO
     * @throws BusinessException Comment의 depth가 대댓글 보다 클 경우{@link ErrorCode#COMMENT_DEPTH_EXCEEDED} 예외 발생
     * @throws BusinessException 대댓글의 댓글이 Post에 속하지않는 경우 {@link ErrorCode#INVALID_INPUT_VALUE} 예외 발생
     * @return CommentResponse
     */
    @Transactional
    public CommentResponse createComment(UUID memberUuid, Long postId, CommentRequest request) {
        Post post = findPostById(postId);
        Member member = findMemberByUuid(memberUuid);

        Comment parent = null;
        if (request.parentId() != null) {
            parent = findCommentById(request.parentId());

            //대댓글의 대댓글 방지 (1단계 depth 제한)
            if (!parent.isRoot()) {
                throw new BusinessException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
            }

            //부모 댓글이 같은 게시글에 속하는지 검증
            if (!parent.getPost().getId().equals(postId)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .member(member)
                .content(request.content())
                .parent(parent)
                .build();

        commentRepository.save(comment);

        // 게시글의 댓글 카운트 증가
        post.increaseCommentCount();

        return CommentResponse.from(comment, false);
    }

    /**
     * 게시글의 댓글 목록 조회 (루트 댓글 + 대댓글 트리 구조)
     * @param postId 조회할 댓글이 포함되어있는 Post의 ID
     * @param memberUuid 현재 사용자의 좋아요 유무를 확인하기 위한 Member의 UUID
     * @return CommentResponse List
     */
    public List<CommentResponse> getComments(Long postId, UUID memberUuid) {
        Post post = findPostById(postId);
        Member member = (memberUuid != null) ? findMemberByUuid(memberUuid) : null;

        // 1. 게시글의 전체 댓글 한 번에 조회 (root + children, 쿼리 1번)
        List<Comment> allComments = commentRepository.findAllByPost(post);

        // 2. 회원이 좋아요한 댓글 ID 목록 한 번에 조회 (쿼리 1번)
        Set<Long> likedCommentIds = (member != null)
                ? commentLikeRepository.findLikedCommentIdsByMemberAndPost(member, post)
                : Set.of();

        // 3. 대댓글을 부모 ID 기준으로 그루핑 (Java 메모리에서 처리, 추가 쿼리 없음)
        Map<Long, List<Comment>> childrenMap = allComments.stream()
                .filter(c -> !c.isRoot())
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // 4. 루트 댓글 기준으로 트리 조립
        return allComments.stream()
                .filter(Comment::isRoot)
                .map(root -> {
                    boolean rootLikedByMe = likedCommentIds.contains(root.getId());

                    List<CommentResponse> children = childrenMap
                            .getOrDefault(root.getId(), List.of())
                            .stream()
                            .map(child -> CommentResponse.from(child, likedCommentIds.contains(child.getId())))
                            .toList();

                    return CommentResponse.fromWithChildren(root, rootLikedByMe, children);
                })
                .toList();
    }

    /**
     * 댓글 수정
     * @param memberUuid 댓글을 수정을 요청한 Member의 UUID
     * @param commentId 수정할 댓글의 Id
     * @param request 댓글 수정 요청 DTO
     * @throws BusinessException 댓글의 소유주가 아닌 경우{@link ErrorCode#ACCESS_DENIED} 예외 발생
     * @return CommentResponse
     */
    @Transactional
    public CommentResponse updateComment(UUID memberUuid, Long commentId, CommentRequest request) {
        Comment comment = findCommentById(commentId);

        if (!comment.isOwner(memberUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        comment.updateContent(request.content());

        Member member = findMemberByUuid(memberUuid);
        boolean likedByMe = commentLikeRepository.existsByCommentAndMember(comment, member);

        return CommentResponse.from(comment, likedByMe);
    }

    /**
     * 댓글 삭제 - 루트 댓글 삭제 시 대댓글도 함께 삭제됨 (CascadeType.ALL)
     * @param memberUuid 댓글을 삭제를 요청한 Member의 UUID
     * @param commentId 삭제할 댓글의 Id
     * @throws BusinessException 댓글의 소유주가 아닌 경우{@link ErrorCode#ACCESS_DENIED} 예외 발생
     */
    @Transactional
    public void deleteComment(UUID memberUuid, Long commentId) {
        Comment comment = findCommentById(commentId);
        Post post = comment.getPost();

        if (!comment.isOwner(memberUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        //삭제될 댓글 수 계산 (본인 + 대댓글)
        int deleteCount = 1 + comment.getChildren().size();

        commentLikeRepository.deleteAllByCommentOrChildren(comment);
        commentRepository.delete(comment);
        post.decreaseCommentCount(deleteCount);
    }

    // ───────────────────── 마이페이지 ─────────────────────


    /**
     * 내가 작성한 댓글 목록 (루트 + 대댓글 모두, 최신순)
     * @param memberUuid 내가 작성한 댓글을 조회할 Member의 UUID
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return CommentResponse Page
     */
    public Page<CommentResponse> getMyComments(UUID memberUuid, Pageable pageable) {
        Member member = findMemberByUuid(memberUuid);

        Page<Comment> comments = commentRepository.findAllByMemberOrderByCreatedAtDesc(member, pageable);

        List<Long> commentIds = comments.stream()
                .map(Comment::getId)
                .toList();

        Set<Long> likedIds = commentIds.isEmpty()
                ? Set.of()
                : commentLikeRepository.findLikedCommentIdsByMemberAndIds(member, commentIds);

        return comments.map(comment -> CommentResponse.from(comment, likedIds.contains(comment.getId())));
    }


    // ───────────────────── 좋아요 ─────────────────────

    /**
     * 댓글 좋아요 토글
     * @param memberUuid 좋아요할 혹은 좋아요 취소할  Member의 UUID
     * @param commentId 좋아요할 혹은 좋아요 취소할 댓글의 ID
     * @return 좋아요라면 true, 좋아요 취소라면 false반환
     */
    @Transactional
    public boolean toggleLike(UUID memberUuid, Long commentId) {
        Comment comment = findCommentById(commentId);
        Member member = findMemberByUuid(memberUuid);

        return commentLikeRepository.findByCommentAndMember(comment, member)
                .map(existingLike -> {
                    commentLikeRepository.delete(existingLike);
                    comment.decreaseLikeCount();
                    return false;
                })
                .orElseGet(() -> {
                    CommentLike commentLike = CommentLike.builder()
                            .comment(comment)
                            .member(member)
                            .build();
                    commentLikeRepository.save(commentLike);
                    comment.increaseLikeCount();
                    return true;
                });
    }

    // ───────────────────── 내부 헬퍼 ─────────────────────

    /**
     * 게시글 조회
     * @param postId 조회할 Post의 ID
     * @throws BusinessException Post가 존재하지 않는 경우{@link ErrorCode#POST_NOT_FOUND} 예외 발생
     * @return Post
     */
    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    /**
     * 댓글 조회
     * @param commentId 조회할 Comment의 ID
     * @throws BusinessException Comment가 존재하지 않는 경우{@link ErrorCode#COMMENT_NOT_FOUND} 예외 발생
     * @return Comment
     */
    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    /**
     * 회원 조회
     * @param memberUuid 조회할 Member의 UUID
     * @throws BusinessException Member가 존재하지 않는 경우{@link ErrorCode#MEMBER_NOT_FOUND} 예외 발생
     * @return Member
     */
    private Member findMemberByUuid(UUID memberUuid) {
        return memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
