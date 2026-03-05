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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

//댓글 다이어리 관련 API 컨트롤러
@Tag(name = "Comment API", description = "댓글 CRUD 및 좋아요 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    private final CommentService commentService;

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

    @Operation(summary = "댓글 목록 조회 (트리 구조)")
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long postId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(commentService.getComments(postId, memberUuid));
    }

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

    // ===================== 마이페이지  =====================

    @Operation(summary = "내가 작성한 댓글 목록 (최신순)")
    @GetMapping("/api/comments/me")
    public ResponseEntity<Page<CommentResponse>> getMyComments(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(commentService.getMyComments(memberUuid, pageable));
    }
}
