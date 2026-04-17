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

    /**
     * 내가 작성한 댓글 목록 조회
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return 200 OK - 내가 작성한 댓글 목록 Page
     * @see CommentService#getMyComments(UUID, Pageable)
     */
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