package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.dto.community.CommentResponse;
import com.zerozoa.skinner.global.util.SecurityUtils;
import com.zerozoa.skinner.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "My Comment API", description = "내 댓글 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class MyCommentController {

    private final CommentService commentService;

    @Operation(summary = "내가 작성한 댓글 목록 (최신순)")
    @GetMapping("/me")
    public ResponseEntity<Page<CommentResponse>> getMyComments(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(commentService.getMyComments(memberUuid, pageable));
    }
}