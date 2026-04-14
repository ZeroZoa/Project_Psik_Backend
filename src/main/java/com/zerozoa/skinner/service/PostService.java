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


    /**
     * 게시글 작성
     * @param memberUuid 게시글을 작성할 회원의 UUID
     * @param request 게시글 생성 요청 DTO
     * @param images 작성될 게시글의 이미지 리스트
     * @throws BusinessException Member가 존재하지 않는 경우 {@link ErrorCode#MEMBER_NOT_FOUND} 예외 발생
     * @return 작성된 게시글 PostResponse
     */
    @Transactional
    public PostResponse createPost(UUID memberUuid, PostRequest request, List<MultipartFile> images) {
        Member member = memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

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

    /**
     * 게시글 목록 조회 - 최신순
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return 게시글 목록 Page (최신순)
     */
    public Page<PostResponse> getPosts(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(PostResponse::fromList);
    }

    /**
     * 게시글 목록 조회 - 추천순
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return 게시글 목록 Page (최신순)
     */
    public Page<PostResponse> getPostsByLikes(Pageable pageable) {
        return postRepository.findAllByOrderByLikeCountDescCreatedAtDesc(pageable)
                .map(PostResponse::fromList);
    }

    /**
     * 게시글 목록 조회 - 조회순
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return 게시글 목록 Page (최신순)
     */
    public Page<PostResponse> getPostsByViews(Pageable pageable) {
        return postRepository.findAllByOrderByViewCountDescCreatedAtDesc(pageable)
                .map(PostResponse::fromList);
    }

    /**
     * 게시글 상세 조회
     * @param postId 조회할 게시글의 ID
     * @param memberUuid 좋아요 여부 확인을 위한 회원의 UUID
     * @return 게시글 상세 내용
     */
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

    /**
     * 게시글 수정
     * @param memberUuid 수정 요청한 Member의 UUID
     * @param postId 수정할 Post의 ID
     * @param request 게시글 수정 요청 DTO
     * @param images 수정될 게시글의 이미지 리스트 (null 또는 빈 리스트 허용)
     * @throws BusinessException 게시글을 찾을 수 없는 경우 {@link ErrorCode#POST_NOT_FOUND} 예외 발생
     * @throws BusinessException 게시글의 소유자가 아닌 경우 {@link ErrorCode#ACCESS_DENIED} 예외 발생
     * @return PostResponse
     */
    @Transactional
    public PostResponse updatePost(UUID memberUuid, Long postId, PostRequest request, List<MultipartFile> images) {
        //게시글 조회 + 소유자 확인
        Post post = findPostById(postId);
        if (!post.isOwner(memberUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        //게시글 수정
        post.update(request.title(), request.content());

        // 기존 이미지 파일 삭제
        List<String> oldImageUrls = post.getImages().stream()
                .map(PostImage::getImageUrl)
                .toList();
        fileStorageService.deleteAll(oldImageUrls);

        //기존 이미지 엔티티 제거 후 새 이미지 저장
        post.replaceImages(new ArrayList<>());

        //이미지 리스트 확인 및 유효성 검사 및 갯수 검사
        if (images != null && !images.isEmpty()) {
            validateImageCount(images.size());
            saveImages(post, images);
        }

        //좋아요 눌렀는지 확인
        Member member = findMemberByUuid(memberUuid);
        boolean likedByMe = postLikeRepository.existsByPostAndMember(post, member);

        return PostResponse.fromDetail(post, likedByMe);
    }

    /**
     * 게시글 삭제
     * @param memberUuid 삭제 요청한 Member의 UUID
     * @param postId 삭제할 Post의 ID
     * @throws BusinessException 게시글을 찾을 수 없는 경우 {@link ErrorCode#POST_NOT_FOUND} 예외 발생
     * @throws BusinessException 게시글의 소유자가 아닌 경우 {@link ErrorCode#ACCESS_DENIED} 예외 발생
     */
    @Transactional
    public void deletePost(UUID memberUuid, Long postId) {
        //게시글 조회 + 소유자 확인
        Post post = findPostById(postId);
        if (!post.isOwner(memberUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 이미지 파일 삭제
        List<String> imageUrls = post.getImages().stream()
                .map(PostImage::getImageUrl)
                .toList();
        fileStorageService.deleteAll(imageUrls);

        //댓글, 좋아요, 게시글 순차적으로 삭제(연관관계)
        commentRepository.deleteAllByPost(post);
        postLikeRepository.deleteAllByPost(post);
        postRepository.delete(post);
    }

    /**
     * 게시글 검색 - (조회)
     * @param keyword 게시글을 검색할 키워드
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return 키워드에 해당하는 게시글 목록 (Page)
     */
    public Page<PostResponse> searchPosts(String keyword, Pageable pageable) {
        return postRepository.searchByKeyword(keyword, pageable)
                .map(PostResponse::fromList);
    }


    // ───────────────────── 마이페이지 ─────────────────────

    /**
     * 내가 쓴 게시글 검색 - (조회)
     * @param memberUuid 조회할 회원의 UUID
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @throws BusinessException 회원을 찾을 수 없는 경우 {@link ErrorCode#MEMBER_NOT_FOUND}
     * @return 내가 작성한 게시글 목록 (Page)
     */
    public Page<PostResponse> getMyPosts(UUID memberUuid, Pageable pageable) {
        Member member = findMemberByUuid(memberUuid);
        return postRepository.findByMemberOrderByCreatedAtDesc(member, pageable)
                .map(PostResponse::fromList);
    }

    /**
     * 내가 좋아요한 게시글 검색 - (조회)
     * @param memberUuid 조회할 회원의 UUID
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @throws BusinessException 회원을 찾을 수 없는 경우 {@link ErrorCode#MEMBER_NOT_FOUND}
     * @return 내가 좋아요한 게시글 목록 (Page)
     */
    public Page<PostResponse> getMyLikedPosts(UUID memberUuid, Pageable pageable) {
        Member member = findMemberByUuid(memberUuid);
        return postRepository.findLikedPostsByMember(member, pageable)
                .map(PostResponse::fromList);
    }

    /**
     * 내가 댓글 단 게시글 검색 - (조회)
     * @param memberUuid 조회할 회원의 UUID
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @throws BusinessException 회원을 찾을 수 없는 경우 {@link ErrorCode#MEMBER_NOT_FOUND}
     * @return 내가 댓글 단 게시글 목록 (Page)
     */
    public Page<PostResponse> getMyCommentedPosts(UUID memberUuid, Pageable pageable) {
        Member member = findMemberByUuid(memberUuid);
        return postRepository.findCommentedPostsByMember(member, pageable)
                .map(PostResponse::fromList);
    }

    // ===================== 좋아요 =====================

    /**
     * 좋아요 버튼
     * @param memberUuid 좋아요 누른 회원의 UUID
     * @param postId 좋아요할 게시글의 ID
     * @throws BusinessException 게시글을 찾을 수 없는 경우 {@link ErrorCode#POST_NOT_FOUND}
     * @throws BusinessException 회원을 찾을 수 없는 경우 {@link ErrorCode#MEMBER_NOT_FOUND}
     * @return 좋아요 -> true, 철회 -> false 반환
     */
    @Transactional
    public boolean toggleLike(UUID memberUuid, Long postId) {
        Post post = findPostById(postId);
        Member member = findMemberByUuid(memberUuid);

        //회원과 게시글을 조회하여 이미 좋아요한 회원이라면 좋아요 삭제, 아직 좋아요하지 않은 회원이라면 생성
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

    // ───────────────────── 내부 헬퍼 ─────────────────────

    /**
     * 게시글 찾기 - (조회)
     * @param postId 찾을 게시글의 ID
     * @throws BusinessException Post가 존재하지 않는 경우 {@link ErrorCode#POST_NOT_FOUND} 예외 발생
     * @return Post
     */
    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    /**
     * 회원 찾기 - (조회)
     * @param memberUuid 찾을 회원의 UUID
     * @throws BusinessException Member가 존재하지 않는 경우 {@link ErrorCode#MEMBER_NOT_FOUND} 예외 발생
     * @return Member
     */
    private Member findMemberByUuid(UUID memberUuid) {
        return memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 게시글과 저장될 이미지 최대 장수 확인
     * @param count 게시글에 저장될 이미지 장수
     * @throws BusinessException 이미지 장수가 최대값을 뛰어넘을때 {@link ErrorCode#IMAGE_LIMIT_EXCEEDED} 예외 발생
     */
    private void validateImageCount(int count) {
        if (count > MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
    }

    /**
     * 이미지 저장
     * @param post 이미지를 저장할 게시글
     * @param images 저장될 이미지
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