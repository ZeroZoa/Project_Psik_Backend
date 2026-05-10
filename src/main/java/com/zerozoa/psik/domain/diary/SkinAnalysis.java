package com.zerozoa.psik.domain.diary;

import com.zerozoa.psik.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 피부 분석 결과 엔티티
 * SkinDiary와 1:1 관계이며, 다이어리당 분석 결과는 1개
 * 생성 시 AnalysisStatus.PENDING으로 시작하고, AI 분석 완료/실패 시 상태 전환
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "skin_analysis")
public class SkinAnalysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skin_analysis_id")
    private Long id;

    // SkinDiary와 1:1 연결 (다이어리당 분석 결과 1개)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skin_diary_id", nullable = false, unique = true)
    private SkinDiary skinDiary;

    // 분석한 이미지 URL
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    // 여드름 심각도 0~100
    @Column(name = "acne_score")
    private Integer acneScore;

    // 주름 점수 0~100
    @Column(name = "wrinkle_score")
    private Integer wrinkleScore;

    // 피부톤 균일도 0~100
    @Column(name = "tone_score")
    private Integer toneScore;

    // 유분 점수 0~100
    @Column(name = "oil_score")
    private Integer oilScore;

    // AI 한줄 요약
    @Column(name = "summary", length = 500)
    private String summary;

    // 분석 상태 (PENDING / COMPLETED / FAILED)
    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false)
    private AnalysisStatus analysisStatus;

    @Builder
    public SkinAnalysis(SkinDiary skinDiary, String imageUrl) {
        this.skinDiary = skinDiary;
        this.imageUrl = imageUrl;
        this.analysisStatus = AnalysisStatus.PENDING;
    }

    /** AI 분석 완료 후 결과값 저장 및 상태를 COMPLETED로 전환 */
    public void completeAnalysis(Integer acneScore,
                                 Integer wrinkleScore, Integer toneScore,
                                 Integer oilScore, String summary) {
        this.acneScore = acneScore;
        this.wrinkleScore = wrinkleScore;
        this.toneScore = toneScore;
        this.oilScore = oilScore;
        this.summary = summary;
        this.analysisStatus = AnalysisStatus.COMPLETED;
    }

    /** AI 분석 실패 시 상태를 FAILED로 전환 */
    public void failAnalysis() {
        this.analysisStatus = AnalysisStatus.FAILED;
    }
}