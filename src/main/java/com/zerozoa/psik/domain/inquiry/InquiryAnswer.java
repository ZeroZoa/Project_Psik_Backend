package com.zerozoa.psik.domain.inquiry;

import com.zerozoa.psik.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inquiry_answer")
public class InquiryAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false, unique = true)
    private Inquiry inquiry;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public InquiryAnswer(Inquiry inquiry, String content) {
        this.inquiry = inquiry;
        this.content = content;
    }

    /** 관리자가 답변 내용 수정 */
    public void updateContent(String content) {
        this.content = content;
    }
}