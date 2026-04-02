package com.zerozoa.skinner.service;

import com.zerozoa.skinner.domain.community.Post;
import com.zerozoa.skinner.domain.community.PostImage;
import com.zerozoa.skinner.domain.community.PostLike;
import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.dto.community.PostRequest;
import com.zerozoa.skinner.dto.community.PostResponse;
import com.zerozoa.skinner.global.exception.BusinessException;
import com.zerozoa.skinner.global.exception.ErrorCode;
import com.zerozoa.skinner.repository.community.CommentRepository;
import com.zerozoa.skinner.repository.community.PostLikeRepository;
import com.zerozoa.skinner.repository.community.PostRepository;
import com.zerozoa.skinner.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final int MAX_IMAGE_COUNT = 5;

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;

    // ===================== CRUD =====================

    @Transactional
    public PostResponse createPost(UUID memberUuid, PostRequest request, List<MultipartFile> images) {
        Member member = findMemberByUuid(memberUuid);

        Post post = Post.builder()
                .member(member)
                .title(request.title())
                .content(request.content())
                .build();

        if (images != null && !images.isEmpty()) {
            validateImageCount(images.size());
            saveImages(post, images);
        }

        Post savedPost = postRepository.save(post);
        return PostResponse.fromDetail(savedPost, false);
    }

    public Page<PostResponse> getPosts(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(PostResponse::fromList);
    }

    public Page<PostResponse> getPostsByLikes(Pageable pageable) {
        return postRepository.findAllByOrderByLikeCountDescCreatedAtDesc(pageable)
                .map(PostResponse::fromList);
    }

    public Page<PostResponse> getPostsByViews(Pageable pageable) {
        return postRepository.findAllByOrderByViewCountDescCreatedAtDesc(pageable)
                .map(PostResponse::fromList);
    }

    @Transactional
    public PostResponse getPost(Long postId, UUID memberUuid) {
        Post post = findPostById(postId);
        post.increaseViewCount();

        // 비로그인 사용자는 likedByMe = false
        boolean likedByMe = false;
        if (memberUuid != null) {
            Member member = findMemberByUuid(memberUuid);
            likedByMe = postLikeRepository.existsByPostAndMember(post, member);
        }

        return PostResponse.fromDetail(post, likedByMe);
    }

    @Transactional
    public PostResponse updatePost(UUID memberUuid, Long postId, PostRequest request, List<MultipartFile> images) {
        Post post = findPostById(postId);

        if (!post.isOwner(memberUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        post.update(request.title(), request.content());

        // 기존 이미지 파일 삭제
        List<String> oldImageUrls = post.getImages().stream()
                .map(PostImage::getImageUrl)
                .toList();
        fileStorageService.deleteAll(oldImageUrls);

        // 기존 이미지 엔티티 제거 후 새 이미지 저장
        post.replaceImages(new ArrayList<>());

        if (images != null && !images.isEmpty()) {
            validateImageCount(images.size());
            saveImages(post, images);
        }

        Member member = findMemberByUuid(memberUuid);
        boolean likedByMe = postLikeRepository.existsByPostAndMember(post, member);

        return PostResponse.fromDetail(post, likedByMe);
    }

    @Transactional
    public void deletePost(UUID memberUuid, Long postId) {
        Post post = findPostById(postId);

        if (!post.isOwner(memberUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 이미지 파일 삭제
        List<String> imageUrls = post.getImages().stream()
                .map(PostImage::getImageUrl)
                .toList();
        fileStorageService.deleteAll(imageUrls);

        commentRepository.deleteAllByPost(post);
        postLikeRepository.deleteAllByPost(post);
        postRepository.delete(post);
    }

    public Page<PostResponse> searchPosts(String keyword, Pageable pageable) {
        return postRepository.searchByKeyword(keyword, pageable)
                .map(PostResponse::fromList);
    }

    // ===================== 마이페이지 =====================

    public Page<PostResponse> getMyPosts(UUID memberUuid, Pageable pageable) {
        Member member = findMemberByUuid(memberUuid);
        return postRepository.findByMemberOrderByCreatedAtDesc(member, pageable)
                .map(PostResponse::fromList);
    }

    public Page<PostResponse> getMyLikedPosts(UUID memberUuid, Pageable pageable) {
        Member member = findMemberByUuid(memberUuid);
        return postRepository.findLikedPostsByMember(member, pageable)
                .map(PostResponse::fromList);
    }

    public Page<PostResponse> getMyCommentedPosts(UUID memberUuid, Pageable pageable) {
        Member member = findMemberByUuid(memberUuid);
        return postRepository.findCommentedPostsByMember(member, pageable)
                .map(PostResponse::fromList);
    }

    // ===================== 좋아요 =====================

    @Transactional
    public boolean toggleLike(UUID memberUuid, Long postId) {
        Post post = findPostById(postId);
        Member member = findMemberByUuid(memberUuid);

        return postLikeRepository.findByPostAndMember(post, member)
                .map(existingLike -> {
                    postLikeRepository.delete(existingLike);
                    post.decreaseLikeCount();
                    return false;
                })
                .orElseGet(() -> {
                    PostLike postLike = PostLike.builder()
                            .post(post)
                            .member(member)
                            .build();
                    postLikeRepository.save(postLike);
                    post.increaseLikeCount();
                    return true;
                });
    }

    // ===================== 내부 헬퍼 =====================

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private Member findMemberByUuid(UUID memberUuid) {
        return memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateImageCount(int count) {
        if (count > MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
    }

    /**
     * MultipartFile 목록 → 파일 저장 + PostImage 엔티티 생성
     */
    private void saveImages(Post post, List<MultipartFile> images) {
        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);
            if (file.isEmpty()) continue;

            String imageUrl = fileStorageService.store(file, "posts");

            PostImage postImage = PostImage.builder()
                    .post(post)
                    .imageUrl(imageUrl)
                    .sortOrder(i)
                    .originalFilename(file.getOriginalFilename())
                    .build();

            post.addImage(postImage);
        }
    }
}