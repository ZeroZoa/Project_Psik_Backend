package com.zerozoa.psik.domain.inquiry;

import com.zerozoa.psik.domain.common.BaseTimeEntity;
import com.zerozoa.psik.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 1:1 문의 답변 엔티티
 * Inquiry와 1:1 관계이며, 문의당 답변은 1개
 * Inquiry.answer(cascade ALL)에 종속되어 문의 삭제 시 함께 삭제됨
 */

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inquiry", indexes = {
        @Index(name = "idx_inquiry_member", columnList = "member_id"),
        @Index(name = "idx_inquiry_created_at", columnList = "created_at")
})
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 답변 — null이면 접수중, 존재하면 답변완료
    @OneToOne(mappedBy = "inquiry", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private InquiryAnswer answer;

    @Builder
    public Inquiry(Member member, String title, String content) {
        this.member = member;
        this.title = title;
        this.content = content;
    }

    /** 답변 완료 여부 — answer 존재 시 true */
    public boolean isAnswered() {
        return this.answer != null;
    }
}