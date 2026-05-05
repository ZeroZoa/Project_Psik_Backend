package com.zerozoa.psik.dto.diary;

import com.zerozoa.psik.domain.diary.AnalysisStatus;
import com.zerozoa.psik.domain.diary.SkinAnalysis;

import java.time.Instant;

// 피부 분석 결과 응답 DTO
public record SkinAnalysisResponse(
        Long skinAnalysisId,
        Long skinDiaryId,
        String imageUrl,
        Integer acneScore,
        Integer wrinkleScore,
        Integer toneScore,
        Integer oilScore,
        String summary,
        AnalysisStatus analysisStatus,
        Instant createdAt
) {
    public static SkinAnalysisResponse from(SkinAnalysis analysis) {
        return new SkinAnalysisResponse(
                analysis.getId(),
                analysis.getSkinDiary().getId(),
                analysis.getImageUrl(),
                analysis.getAcneScore(),
                analysis.getWrinkleScore(),
                analysis.getToneScore(),
                analysis.getOilScore(),
                analysis.getSummary(),
                analysis.getAnalysisStatus(),
                analysis.getCreatedAt()
        );
    }
}