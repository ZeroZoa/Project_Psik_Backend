package com.zerozoa.psik.controller;

import com.zerozoa.psik.dto.community.PostHomeResponse;
import com.zerozoa.psik.dto.community.PostRequest;
import com.zerozoa.psik.dto.community.PostResponse;
import com.zerozoa.psik.global.util.SecurityUtils;
import com.zerozoa.psik.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Validated
@Tag(name = "Post API", description = "게시글 CRUD 및 좋아요 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

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
        log.info("[API] 게시글 작성 요청 - memberUuid={}", memberUuid);
        PostResponse response = postService.createPost(memberUuid, request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 게시글 상세 조회 (비로그인 사용자도 조회 가능)
     * @param principal Spring Security Context에 저장된 인증 객체 (비로그인 시 null)
     * @param postId 조회할 게시글의 ID
     * @return 200 OK - 게시글 상세 정보 (로그인 시 좋아요 여부 포함)
     * @see PostService#getPost(Long, UUID)
     */
    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long postId
    ) {
        // 비로그인 사용자도 상세 조회 가능 (principal이 null일 수 있음)
        UUID memberUuid = (principal instanceof UUID) ? (UUID) principal : null;
        return ResponseEntity.ok(postService.getPost(postId, memberUuid));
    }

    /**
     * 홈 화면용 게시글 조회 (비로그인 사용자도 조회 가능)
     * @return 200 OK - 홈 화면용 게시글
     * @see PostService#getHomePosts()
     */
    @Operation(summary = "커뮤니티 홈 섹션 (HOT/NEW/POPULAR)")
    @GetMapping()
    public ResponseEntity<PostHomeResponse> getHomePosts() {
        return ResponseEntity.ok(postService.getHomePosts());
    }

    /**
     * 홈 화면용 HOT 게시글 전체 목록 (비로그인 사용자도 조회 가능)
     * @return 200 OK - 홈 화면용 HOT 게시글 전체 목록
     * @see PostService#getHotPosts(Pageable)
     */
    @Operation(summary = "HOT 게시글 전체 목록 (최근 7일 좋아요 순)")
    @GetMapping("/hot")
    public ResponseEntity<Page<PostResponse>> getHotPosts(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(postService.getHotPosts(pageable));
    }


    /**
     * 홈 화면용 NEW 게시글 전체 목록 (비로그인 사용자도 조회 가능)
     * @return 200 OK - 홈 화면용 NEW 게시글 전체 목록
     * @see PostService#getNewPosts(Pageable)
     */
    @Operation(summary = "NEW 게시글 전체 목록 (최신순)")
    @GetMapping("/new")
    public ResponseEntity<Page<PostResponse>> getNewPosts(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(postService.getNewPosts(pageable));
    }

    /**
     * 홈 화면용 POPULAR 게시글 전체 목록 (비로그인 사용자도 조회 가능)
     * @return 200 OK - 홈 화면용 POPULAR 게시글 전체 목록
     * @see PostService#getPopularPosts(Pageable)
     */
    @Operation(summary = "POPULAR 게시글 전체 목록 (최근 7일 조회수 순)")
    @GetMapping("/popular")
    public ResponseEntity<Page<PostResponse>> getPopularPosts(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(postService.getPopularPosts(pageable));
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
        log.info("[API] 게시글 수정 요청 - memberUuid={}, postId={}", memberUuid, postId);
        return ResponseEntity.ok(postService.updatePost(memberUuid, postId, request, images));
    }

    /**
     * 게시글 삭제 - 연관관계도 삭제확인
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param postId  삭제할 게시글의 ID
     * @return 204 No Content
     */
    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long postId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        log.warn("[API] 게시글 삭제 요청 - memberUuid={}, postId={}", memberUuid, postId);
        postService.deletePost(memberUuid, postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글 키워드 검색
     * @param keyword 검색할 키워드 (제목 + 내용 대상)
     * @param pageable 페이지네이션 정보 (기본값: size=20)
     * @return 200 OK - 검색 결과 게시글 목록 (페이지)
     * @see PostService#searchPosts(String, Pageable)
     */
    @Operation(summary = "게시글 검색")
    @GetMapping("/search")
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @RequestParam @NotBlank String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(postService.searchPosts(keyword, pageable));
    }

    // ===================== 마이페이지 =====================

    /**
     * 내가 작성한 게시글 목록 조회
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param pageable 페이지네이션 정보 (기본값: size=20)
     * @return 200 OK - 내가 작성한 게시글 목록 (최신순, 페이지)
     * @see PostService#getMyPosts(UUID, Pageable)
     */
    @Operation(summary = "내가 작성한 게시글 목록")
    @GetMapping("/me")
    public ResponseEntity<Page<PostResponse>> getMyPosts(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(postService.getMyPosts(memberUuid, pageable));
    }

    /**
     * 내가 좋아요 누른 게시글 목록 조회
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param pageable 페이지네이션 정보 (기본값: size=20)
     * @return 200 OK - 내가 좋아요한 게시글 목록 (페이지)
     * @see PostService#getMyLikedPosts(UUID, Pageable)
     */
    @Operation(summary = "내가 좋아요 누른 게시글 목록")
    @GetMapping("/me/liked")
    public ResponseEntity<Page<PostResponse>> getMyLikedPosts(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(postService.getMyLikedPosts(memberUuid, pageable));
    }

    /**
     * 내가 댓글 단 게시글 목록 조회
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param pageable 페이지네이션 정보 (기본값: size=20)
     * @return 200 OK - 내가 댓글을 작성한 게시글 목록 (페이지)
     * @see PostService#getMyCommentedPosts(UUID, Pageable)
     */
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

    /**
     * 게시글 좋아요 토글 (좋아요 ↔ 좋아요 취소)
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param postId 좋아요할 혹은 좋아요 취소할 게시글의 ID
     * @return 200 OK - {"liked": true} 좋아요, {"liked": false} 좋아요 취소
     * @see PostService#toggleLike(UUID, Long)
     */
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