package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.dto.community.PostRequest;
import com.zerozoa.skinner.dto.community.PostResponse;
import com.zerozoa.skinner.global.util.SecurityUtils;
import com.zerozoa.skinner.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Post API", description = "게시글 CRUD 및 좋아요 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    // ===================== CRUD =====================

    /**
     * 게시글 작성 (이미지 포함)
     * Content-Type: multipart/form-data
     * - request: JSON part (title, content)
     * - images: 파일 part (0~5장, 선택)
     */
    @Operation(summary = "게시글 작성 (이미지 포함)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Valid @RequestPart("request") PostRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        PostResponse response = postService.createPost(memberUuid, request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "게시글 목록 조회", description = "sort: latest(기본), likes, views")
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPosts(
            @RequestParam(defaultValue = "latest") String sort,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PostResponse> response = switch (sort) {
            case "likes" -> postService.getPostsByLikes(pageable);
            case "views" -> postService.getPostsByViews(pageable);
            default -> postService.getPosts(pageable);
        };
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long postId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(postService.getPost(postId, memberUuid));
    }

    /**
     * 게시글 수정 (이미지 교체)
     * 기존 이미지 전부 삭제 → 새 이미지로 교체
     */
    @Operation(summary = "게시글 수정 (이미지 교체)")
    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> updatePost(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long postId,
            @Valid @RequestPart("request") PostRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(postService.updatePost(memberUuid, postId, request, images));
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long postId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        postService.deletePost(memberUuid, postId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시글 검색")
    @GetMapping("/search")
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(postService.searchPosts(keyword, pageable));
    }

    // ===================== 마이페이지 =====================

    @Operation(summary = "내가 작성한 게시글 목록")
    @GetMapping("/me")
    public ResponseEntity<Page<PostResponse>> getMyPosts(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(postService.getMyPosts(memberUuid, pageable));
    }

    @Operation(summary = "내가 좋아요 누른 게시글 목록")
    @GetMapping("/me/liked")
    public ResponseEntity<Page<PostResponse>> getMyLikedPosts(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(postService.getMyLikedPosts(memberUuid, pageable));
    }

    @Operation(summary = "내가 댓글 단 게시글 목록")
    @GetMapping("/me/commented")
    public ResponseEntity<Page<PostResponse>> getMyCommentedPosts(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(postService.getMyCommentedPosts(memberUuid, pageable));
    }

    // ===================== 좋아요 =====================

    @Operation(summary = "게시글 좋아요 토글")
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long postId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        boolean liked = postService.toggleLike(memberUuid, postId);
        return ResponseEntity.ok(Map.of("liked", liked));
    }
}