package com.zerozoa.skinner.service;


import com.zerozoa.skinner.domain.community.Comment;
import com.zerozoa.skinner.domain.community.CommentLike;
import com.zerozoa.skinner.domain.community.Post;
import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.dto.community.CommentRequest;
import com.zerozoa.skinner.dto.community.CommentResponse;
import com.zerozoa.skinner.global.exception.BusinessException;
import com.zerozoa.skinner.global.exception.ErrorCode;
import com.zerozoa.skinner.repository.community.CommentLikeRepository;
import com.zerozoa.skinner.repository.community.CommentRepository;
import com.zerozoa.skinner.repository.community.PostRepository;
import com.zerozoa.skinner.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

//댓글 비지니스 로직을 담당하는 서비스
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
     * 대댓글은 1단계만 허용 (대댓글의 대댓글 금지)
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
     */
    public List<CommentResponse> getComments(Long postId, UUID memberUuid) {
        Post post = findPostById(postId);
        Member member = findMemberByUuid(memberUuid);

        //루트 댓글 조회
        List<Comment> rootComments = commentRepository.findRootCommentsByPost(post);

        //각 루트 댓글에 대해 대댓글 조회 + likedByMe 계산
        return rootComments.stream()
                .map(root -> {
                    boolean rootLikedByMe = commentLikeRepository
                            .existsByCommentAndMember(root, member);

                    List<CommentResponse> children = commentRepository
                            .findChildComments(root).stream()
                            .map(child -> {
                                boolean childLikedByMe = commentLikeRepository
                                        .existsByCommentAndMember(child, member);
                                return CommentResponse.from(child, childLikedByMe);
                            })
                            .toList();

                    return CommentResponse.fromWithChildren(root, rootLikedByMe, children);
                })
                .toList();
    }

    /**
     * 댓글 수정
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
     * 댓글 삭제
     * 루트 댓글 삭제 시 대댓글도 함께 삭제됨 (CascadeType.ALL)
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

        //댓글 좋아요 삭제
        commentLikeRepository.deleteAllByComment(comment);
        // 대댓글 좋아요도 삭제
        for (Comment child : comment.getChildren()) {
            commentLikeRepository.deleteAllByComment(child);
        }

        commentRepository.delete(comment);

        // 게시글 댓글 카운트 감소
        for (int i = 0; i < deleteCount; i++) {
            post.decreaseCommentCount();
        }
    }

    // ===================== 마이페이지 =====================

    /**
     * 내가 작성한 댓글 목록 (루트 + 대댓글 모두, 최신순)
     * 각 댓글이 어떤 게시글에 달렸는지도 함께 조회 (fetch join)
     */
    public Page<CommentResponse> getMyComments(UUID memberUuid, Pageable pageable) {
        Member member = findMemberByUuid(memberUuid);

        return commentRepository.findAllByMemberOrderByCreatedAtDesc(member, pageable)
                .map(comment -> {
                    boolean likedByMe = commentLikeRepository
                            .existsByCommentAndMember(comment, member);
                    return CommentResponse.from(comment, likedByMe);
                });
    }

    // ===================== 좋아요 =====================

    /**
     * 댓글 좋아요 토글
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

    // ===================== 내부 헬퍼 =====================

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private Member findMemberByUuid(UUID memberUuid) {
        return memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
