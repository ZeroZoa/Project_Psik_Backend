package com.zerozoa.psik.service.member;

import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.repository.auth.RefreshTokenRepository;
import com.zerozoa.psik.repository.community.CommentLikeRepository;
import com.zerozoa.psik.repository.community.CommentRepository;
import com.zerozoa.psik.repository.community.PostLikeRepository;
import com.zerozoa.psik.repository.community.PostRepository;
import com.zerozoa.psik.repository.contents.MemberProductRepository;
import com.zerozoa.psik.repository.diary.SkinAnalysisRepository;
import com.zerozoa.psik.repository.diary.SkinDiaryProductRepository;
import com.zerozoa.psik.repository.diary.SkinDiaryRepository;
import com.zerozoa.psik.repository.inquiry.InquiryRepository;
import com.zerozoa.psik.repository.member.MemberRepository;
import com.zerozoa.psik.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 단위 테스트에는 활성 트랜잭션이 없으므로 AfterCommit.run은 즉시 실행된다.
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final UUID GHOST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Mock MemberRepository memberRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock CommentLikeRepository commentLikeRepository;
    @Mock MemberProductRepository memberProductRepository;
    @Mock SkinAnalysisRepository skinAnalysisRepository;
    @Mock SkinDiaryRepository skinDiaryRepository;
    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;
    @Mock InquiryRepository inquiryRepository;
    @Mock SkinDiaryProductRepository skinDiaryProductRepository;
    @Mock FileStorageService fileStorageService;
    @InjectMocks MemberService service;

    private UUID memberUuid;
    private Member member;

    @BeforeEach
    void setUp() {
        memberUuid = UUID.randomUUID();
        member = mock(Member.class);
        when(memberRepository.findByUuid(memberUuid)).thenReturn(Optional.of(member));
        when(memberRepository.findByUuid(GHOST_UUID)).thenReturn(Optional.of(mock(Member.class)));
    }

    @Test
    @DisplayName("deleteMember: 확보한 분석 사진 URL을 삭제 대상으로 넘긴다")
    void deleteMember_deletesAnalysisImages() {
        // given
        List<String> urls = List.of("https://storage.example.com/analysis/a.webp",
                "https://storage.example.com/analysis/b.webp");
        when(skinAnalysisRepository.findImageUrlsByMember(member)).thenReturn(urls);

        // when
        service.deleteMember(memberUuid);

        // then
        verify(memberRepository).delete(member);
        verify(fileStorageService).deleteAll(urls);
    }

    @Test
    @DisplayName("deleteMember: 분석 행을 지우기 전에 사진 URL을 먼저 조회한다")
    void deleteMember_fetchesUrlsBeforeDeletingRows() {
        // given
        when(skinAnalysisRepository.findImageUrlsByMember(member)).thenReturn(List.of("url"));

        // when
        service.deleteMember(memberUuid);

        // then: 순서가 뒤바뀌면 URL을 조회할 수 없다
        InOrder inOrder = inOrder(skinAnalysisRepository);
        inOrder.verify(skinAnalysisRepository).findImageUrlsByMember(member);
        inOrder.verify(skinAnalysisRepository).deleteAllBySkinDiary_Member(member);
    }

    @Test
    @DisplayName("deleteMember: 분석 사진이 없는 회원은 빈 목록으로 처리한다")
    void deleteMember_noImages() {
        // given
        when(skinAnalysisRepository.findImageUrlsByMember(member)).thenReturn(List.of());

        // when
        service.deleteMember(memberUuid);

        // then
        verify(memberRepository).delete(member);
        verify(fileStorageService).deleteAll(List.of());
    }
}
