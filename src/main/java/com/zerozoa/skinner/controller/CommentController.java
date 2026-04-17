package com.zerozoa.skinner.controller;


import com.zerozoa.skinner.dto.community.CommentRequest;
import com.zerozoa.skinner.dto.community.CommentResponse;
import com.zerozoa.skinner.global.util.SecurityUtils;
import com.zerozoa.skinner.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Comment API", description = "댓글 CRUD 및 좋아요 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    private final CommentService commentService;

    /**
     * 댓글 작성 - 생성
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입, 비로그인 시 null)
     * @param postId 댓글을 작성할 게시글의 ID
     * @param request 댓글 작성 요청
     * @return 201 Created
     * @see CommentService#createComment(UUID, Long, CommentRequest)
     */
    @Operation(summary = "댓글 작성 (루트 댓글 또는 대댓글)")
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        CommentResponse response = commentService.createComment(memberUuid, postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 댓글 목록 조회
     * @param principal Spring Security Context에 저장된 인증 객체
     * @param postId 댓글을 조회할 게시글의 ID
     * @return 200 OK
     * @see CommentService#getComments(Long, UUID)
     */
    @Operation(summary = "댓글 목록 조회 (트리 구조)")
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long postId
    ) {
        // 비로그인 사용자도 댓글 목록 조회 가능
        UUID memberUuid = (principal instanceof UUID) ? (UUID) principal : null;
        return ResponseEntity.ok(commentService.getComments(postId, memberUuid));
    }

    /**
     * 댓글 수정
     * @param principal Spring Security Context에 저장된 인증 객체
     * @param postId 수정할 댓글을 소유한 게시글의 ID
     * @param commentId 수정할 댓글의 ID
     * @param request 댓글 수정 요청
     * @return 200 OK
     * @see CommentService#updateComment(UUID, Long, CommentRequest)
     */
    @Operation(summary = "댓글 수정")
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(commentService.updateComment(memberUuid, commentId, request));
    }

    /**
     * 댓글 삭제
     * @param principal Spring Security Context에 저장된 인증 객체
     * @param postId 삭제할 댓글을 소유한 게시글의 ID
     * @param commentId 삭제할 댓글의 ID
     * @return 204 No Content
     * @see CommentService#deleteComment(UUID, Long)
     */
    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        commentService.deleteComment(memberUuid, commentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 댓글 좋아요 토글
     * @param principal Spring Security Context에 저장된 인증 객체
     * @param postId 댓글이 속한 게시글의 ID
     * @param commentId 좋아요 토글할 댓글의 ID
     * @return 200 OK
     * @see CommentService#toggleLike(UUID, Long)
     */
    @Operation(summary = "댓글 좋아요 토글")
    @PostMapping("/{commentId}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        boolean liked = commentService.toggleLike(memberUuid, commentId);
        return ResponseEntity.ok(Map.of("liked", liked));
    }
}
